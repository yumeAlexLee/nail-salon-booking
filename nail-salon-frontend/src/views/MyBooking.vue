<template>
  <div class="my-booking">
    <van-nav-bar
      :title="$t('booking.myBookings')"
      left-arrow
      @click-left="goBack"
      class="glass-effect"
      fixed placeholder
      :border="false"
    />

    <!-- 联系方式输入区（未登录时显示） -->
    <div v-if="showSearch" class="search-section">
      <div class="search-card frost-card-soft">
        <van-field
          v-model="contactId"
          :placeholder="$t('booking.contactPlaceholder')"
          clearable
          center
          :border="false"
          size="large"
        >
          <template #left-icon>
            <van-icon name="phone-o" size="20" color="var(--pink-500)" />
          </template>
          <template #button>
            <van-button
              size="small"
              type="primary"
              :loading="searching"
              round
              @click="searchReservations"
            >
              {{ $t('booking.search') }}
            </van-button>
          </template>
        </van-field>
      </div>
    </div>

    <!-- 已自动登录时显示切换账号入口 -->
    <div v-if="!showSearch" class="switch-account-row">
      <van-button
        plain round size="mini" type="default"
        icon="exchange"
        @click="showSearch = true"
        class="switch-account-btn"
      >
        切换账号
      </van-button>
    </div>

    <!-- 加载中 -->
    <van-loading v-if="searching" size="24px" vertical style="margin-top: 60px;">
      {{ $t('date.loading') }}
    </van-loading>

    <!-- 没有记录 -->
    <div v-else-if="searched && reservations.length === 0" class="empty-state">
      <van-icon name="info-o" size="48" color="#c8c9cc" />
      <p>{{ $t('booking.noRecords') }}</p>
    </div>

    <!-- 预约列表 -->
    <div v-else-if="reservations.length > 0" class="booking-list">
      <div
        v-for="r in reservations"
        :key="r.id"
        class="booking-card"
        :class="{ 'is-cancelled': r.status === 'CANCELLED', 'is-completed': r.status === 'COMPLETED' }"
      >
        <!-- 卡片头部 -->
        <div class="card-header">
          <div class="card-date">
            <span class="date-label">{{ formatDate(r.reserveDate) }}</span>
            <span class="time-label">{{ r.timeSlot }}</span>
          </div>
          <span :class="['status-badge', statusBadgeClass(r.status)]">
            {{ statusLabel(r.status) }}
          </span>
        </div>

        <!-- 待付定金催付提示 -->
        <div v-if="r.status === 'PENDING_DEPOSIT' && r.depositStatus !== 'CUSTOMER_PAID'" class="deposit-reminder">
          <van-icon name="warning-o" size="14" color="#d48806" />
          <span>请尽快完成定金支付以确认预约</span>
        </div>

        <!-- 卡片详情 -->
        <div class="card-body">
          <div class="info-row">
            <span class="info-key">{{ $t('form.identity') }}</span>
            <span class="info-val">
              {{ r.customerType === 'NEW' ? $t('home.newCustomer') : $t('home.oldCustomer') }}
            </span>
          </div>
          <div class="info-row">
            <span class="info-key">{{ $t('form.removalLabel') }}</span>
            <span class="info-val">{{ r.removalType }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">{{ $t('form.remarksLabel') }}</span>
            <span class="info-val">{{ r.remarks || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-key">定金</span>
            <span class="info-val" :class="depositClass(r.depositStatus)">
              {{ depositLabel(r) }}
            </span>
          </div>
        </div>

        <!-- 操作按钮（PENDING_DEPOSIT 或 CONFIRMED 时显示） -->
        <div class="card-action" v-if="r.status === 'PENDING_DEPOSIT' || r.status === 'CONFIRMED'">
          <van-button
            v-if="r.status === 'CONFIRMED'"
            plain
            round
            size="small"
            type="primary"
            @click="confirmReschedule(r)"
            class="action-btn"
          >
            {{ $t('booking.reschedule') }}
          </van-button>
          <van-button
            v-if="r.depositStatus !== 'CUSTOMER_PAID' && r.depositStatus !== 'PAID'"
            plain
            round
            size="small"
            type="danger"
            @click="confirmCancel(r)"
            class="action-btn"
          >
            {{ $t('booking.cancel') }}
          </van-button>
        </div>
      </div>
    </div>

    <!-- 取消确认弹窗 -->
    <van-dialog
      v-model:show="showCancelDialog"
      :title="$t('booking.cancel')"
      :show-cancel-button="true"
      confirm-button-color="var(--pink-500)"
      @confirm="doCancel"
    >
      <div class="cancel-dialog-content">
        <p>{{ $t('booking.cancelConfirm') }}</p>
      </div>
    </van-dialog>

    <!-- 改期弹窗：选择日期 -->
    <van-calendar
      v-model:show="showRescheduleCalendar"
      :min-date="minDate"
      :max-date="maxDate"
      color="var(--pink-500)"
      @confirm="onCalendarConfirm"
    />

    <!-- 改期弹窗：选择时间段 -->
    <van-action-sheet
      v-model:show="showSlotSheet"
      :title="$t('booking.selectNewTime')"
      :cancel-text="'取消'"
    >
      <div class="slot-list">
        <van-loading v-if="loadingSlots" size="24px" vertical style="padding: 32px;" />
        <div v-else-if="availableSlots.length === 0" class="slot-empty">
          当天暂无可用时间段
        </div>
        <div
          v-else
          v-for="slot in availableSlots"
          :key="slot.timeSlot"
          class="slot-item"
          :class="{ 'slot-disabled': !slot.available }"
          @click="slot.available && selectSlot(slot.timeSlot)"
        >
          <span class="slot-time">{{ slot.timeSlot }}</span>
          <van-tag :type="slot.available ? 'success' : 'default'" size="small" round>
            {{ slot.available ? $t('date.available') : $t('date.full') }}
          </van-tag>
        </div>
      </div>
    </van-action-sheet>

    <!-- 改期确认弹窗 -->
    <van-dialog
      v-model:show="showRescheduleConfirm"
      :title="$t('booking.reschedule')"
      :show-cancel-button="true"
      confirm-button-color="var(--pink-500)"
      @confirm="doReschedule"
    >
      <div class="cancel-dialog-content">
        <p>{{ $t('booking.rescheduleConfirm') }}</p>
        <p v-if="selectedDate && selectedSlot" class="new-time-preview">
          {{ formatDate(selectedDate) }} {{ selectedSlot }}
        </p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { showToast } from 'vant';
import { getReservationsByContact, userCancelReservation, rescheduleReservation, getAvailability } from '../api';

const router = useRouter();
const { t } = useI18n();

const contactId = ref('');
const searching = ref(false);
const searched = ref(false);
const reservations = ref([]);
const showSearch = ref(true);

// 取消逻辑
const showCancelDialog = ref(false);
const cancellingReservation = ref(null);

// 改期逻辑
const reschedulingReservation = ref(null);
const showRescheduleCalendar = ref(false);
const showSlotSheet = ref(false);
const showRescheduleConfirm = ref(false);
const selectedDate = ref('');
const selectedSlot = ref('');
const availableSlots = ref([]);
const loadingSlots = ref(false);

onMounted(() => {
  const storedContact = sessionStorage.getItem('userContactId');
  if (storedContact) {
    contactId.value = storedContact;
    showSearch.value = false;
    searchReservations();
  }
});

const minDate = computed(() => new Date());
const maxDate = computed(() => {
  const d = new Date();
  d.setDate(d.getDate() + 14);
  return d;
});

const goBack = () => router.back();

const formatDate = (dateStr) => {
  if (!dateStr) return '';
  const parts = String(dateStr).split('-');
  if (parts.length === 3) {
    return `${parseInt(parts[1])}月${parseInt(parts[2])}日`;
  }
  return dateStr;
};

const toDateStr = (d) => {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const searchReservations = async () => {
  if (!contactId.value.trim()) {
    showToast(t('booking.contactPlaceholder'));
    return;
  }
  searching.value = true;
  searched.value = true;
  try {
    const res = await getReservationsByContact(contactId.value.trim());
    if (res.code === 200) {
      reservations.value = res.data;
    } else {
      showToast(res.message || t('booking.cancelFailed'));
    }
  } catch (e) {
    showToast(t('booking.cancelFailed'));
  } finally {
    searching.value = false;
  }
};

const statusBadgeClass = (status) => {
  if (status === 'CONFIRMED') return 'confirmed';
  if (status === 'COMPLETED') return 'completed';
  if (status === 'CANCELLED') return 'cancelled';
  if (status === 'PENDING_DEPOSIT') return 'pending';
  return 'pending';
};

const statusLabel = (status) => {
  return t(`booking.status.${status}`) || status;
};

const depositLabel = (r) => {
  const amount = r.depositAmount > 0 ? `¥${r.depositAmount.toLocaleString()}` : '¥500';
  const status = r.depositStatus || 'NONE';
  const label = t(`booking.deposit.${status}`) || status;
  return `${amount} · ${label}`;
};

const depositClass = (status) => {
  if (status === 'PAID') return 'deposit-paid';
  if (status === 'CUSTOMER_PAID') return 'deposit-pending';
  if (status === 'PENDING_REFUND') return 'deposit-pending';
  if (status === 'REFUNDED') return 'deposit-refunded';
  if (status === 'FORFEITED') return 'deposit-forfeited';
  return '';
};

const confirmCancel = (r) => {
  cancellingReservation.value = r;
  showCancelDialog.value = true;
};

const doCancel = async () => {
  if (!cancellingReservation.value) return;
  try {
    const res = await userCancelReservation(cancellingReservation.value.id);
    if (res.code === 200) {
      showToast(t('booking.cancelSuccess'));
      await searchReservations();
    } else {
      showToast(res.message || t('booking.cancelFailed'));
    }
  } catch (e) {
    const msg = e?.response?.data?.message || t('booking.cancelFailed');
    showToast(msg);
  }
  cancellingReservation.value = null;
};

// 改期流程
const confirmReschedule = (r) => {
  reschedulingReservation.value = r;
  showRescheduleCalendar.value = true;
};

const onCalendarConfirm = async (date) => {
  showRescheduleCalendar.value = false;
  selectedDate.value = toDateStr(date);
  loadingSlots.value = true;
  showSlotSheet.value = true;
  try {
    availableSlots.value = await getAvailability(selectedDate.value);
  } catch (e) {
    showToast(t('booking.rescheduleFailed'));
    showSlotSheet.value = false;
  } finally {
    loadingSlots.value = false;
  }
};

const selectSlot = (slot) => {
  selectedSlot.value = slot;
  showSlotSheet.value = false;
  showRescheduleConfirm.value = true;
};

const doReschedule = async () => {
  if (!reschedulingReservation.value || !selectedDate.value || !selectedSlot.value) return;
  try {
    const res = await rescheduleReservation(
      reschedulingReservation.value.id,
      selectedDate.value,
      selectedSlot.value
    );
    if (res.code === 200) {
      showToast(t('booking.rescheduleSuccess'));
      await searchReservations();
    } else {
      showToast(res.message || t('booking.rescheduleFailed'));
    }
  } catch (e) {
    const msg = e?.response?.data?.message || t('booking.rescheduleFailed');
    showToast(msg);
  }
  reschedulingReservation.value = null;
  selectedDate.value = '';
  selectedSlot.value = '';
};
</script>

<style scoped>
.my-booking {
  min-height: 100vh;
  background: var(--surface-bg);
  display: flex;
  flex-direction: column;
}

/* ─────────── Navigation bar frosted glass ─────────── */
.glass-effect {
  background: rgba(253, 243, 245, 0.78) !important;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

/* 切换账号行 */
.switch-account-row {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-2) var(--space-4) 0;
}
.switch-account-btn {
  font-size: var(--fs-12);
  color: var(--ink-500);
}

/* 搜索区域 */
.search-section {
  padding: var(--space-3) var(--space-4);
}

/* 空状态 */
.empty-state {
  text-align: center;
  margin-top: 80px;
  color: var(--ink-500);
}
.empty-state p {
  margin-top: var(--space-3);
  font-size: var(--fs-15);
}

/* 预约列表 */
.booking-list {
  padding: 0 var(--space-4) var(--space-10);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}
.booking-card {
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2);
  overflow: hidden;
  transition: all var(--dur-base) var(--ease-soft);
}
.booking-card.is-cancelled {
  opacity: 0.65;
}
.booking-card.is-completed {
  opacity: 0.75;
}

/* 卡片头部 */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-4) var(--space-4) var(--space-2);
}
.date-label {
  font-size: var(--fs-16);
  font-weight: 600;
  color: var(--ink-700);
}
.time-label {
  font-size: var(--fs-13);
  color: var(--ink-500);
  margin-left: var(--space-2);
}

/* 待付定金提示条 */
.deposit-reminder {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 var(--space-4) var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: #fffbe6;
  border-radius: var(--radius-sm);
  font-size: var(--fs-12);
  color: #d48806;
}

/* 卡片详情 */
.card-body {
  padding: 4px var(--space-4) var(--space-3);
}
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: var(--fs-14);
}
.info-key {
  color: var(--ink-500);
}
.info-val {
  color: var(--ink-700);
  font-weight: 500;
  text-align: right;
}

/* 定金状态颜色 */
.deposit-paid { color: var(--accent-green); }
.deposit-pending { color: var(--pink-500); }
.deposit-refunded { color: var(--ink-400); }
.deposit-forfeited { color: var(--accent-red); }

/* 操作区 */
.card-action {
  padding: 0 var(--space-4) var(--space-3);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
.action-btn {
  font-size: var(--fs-12);
}

/* 取消/改期弹窗 */
.cancel-dialog-content {
  padding: var(--space-4) var(--space-5);
  text-align: center;
  font-size: var(--fs-15);
  color: var(--ink-700);
  line-height: 1.6;
}
.new-time-preview {
  margin-top: var(--space-2);
  font-size: var(--fs-18);
  font-weight: 600;
  color: var(--pink-500);
}

/* 时间段列表 */
.slot-list {
  padding: var(--space-3) var(--space-4) var(--space-8);
}
.slot-empty {
  text-align: center;
  padding: var(--space-8);
  color: var(--ink-500);
  font-size: var(--fs-15);
}
.slot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-soft);
  cursor: pointer;
}
.slot-item:last-child {
  border-bottom: none;
}
.slot-disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.slot-time {
  font-size: var(--fs-16);
  font-weight: 500;
  color: var(--ink-700);
}
</style>
