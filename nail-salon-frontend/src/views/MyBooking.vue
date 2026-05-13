<template>
  <div class="my-booking">
    <!-- ═══ Nav ═══ -->
    <div class="mb-nav">
      <button class="mb-back" @click="goBack">
        <van-icon name="arrow-left" size="18" color="var(--ink-700)" />
      </button>
      <div class="mb-title">我的预约</div>
      <div style="width:36px;"></div>
    </div>

    <!-- 搜索区（未自动登录时） -->
    <div v-if="showSearch" class="search-area">
      <div class="search-box">
        <van-icon name="phone-o" size="16" color="var(--pink-500)" style="flex-shrink:0;" />
        <input v-model="contactId" placeholder="输入预约时填写的联系方式" class="search-input" />
        <button class="search-btn" :loading="searching" @click="searchReservations">查询</button>
      </div>
    </div>

    <!-- 已登录时显示切换入口 -->
    <div v-if="!showSearch && reservations.length > 0" class="switch-row">
      <button class="switch-btn" @click="showSearch = true; reservations = []; searched = false;">
        <van-icon name="exchange" size="12" /> 切换账号
      </button>
    </div>

    <!-- 加载 -->
    <van-loading v-if="searching" size="20px" vertical style="margin-top:80px;">加载中...</van-loading>

    <!-- ═══ 空状态 ═══ -->
    <div v-else-if="searched && reservations.length === 0" class="empty-state">
      <div class="empty-icon">🌷</div>
      <div class="empty-title">还没有预约</div>
      <div class="empty-sub">要来一次吗？</div>
    </div>

    <!-- ═══ 预约列表 ═══ -->
    <div v-else-if="reservations.length > 0" class="booking-list">
      <div v-for="r in reservations" :key="r.id" class="bk-card" :class="{ muted: r.status === 'CANCELLED' || r.status === 'COMPLETED' }">
        <div class="bk-header">
          <div>
            <span class="bk-date">{{ formatDate(r.reserveDate) }}</span>
            <span class="bk-time">{{ r.timeSlot }}</span>
          </div>
          <span :class="['status-badge', statusBadgeClass(r.status)]">{{ statusLabel(r.status) }}</span>
        </div>

        <!-- 催付 -->
        <div v-if="r.status === 'PENDING_DEPOSIT' && r.depositStatus !== 'CUSTOMER_PAID'" class="bk-reminder">
          ⚠️ 请尽快完成定金支付
        </div>

        <div class="bk-body">
          <div class="bk-row"><span class="rk">身份</span><span class="rv">{{ r.customerType === 'NEW' ? '新客' : '老客' }}</span></div>
          <div class="bk-row"><span class="rk">卸甲</span><span class="rv">{{ r.removalType || '-' }}</span></div>
          <div class="bk-row"><span class="rk">备注</span><span class="rv">{{ r.remarks || '-' }}</span></div>
          <div class="bk-row"><span class="rk">定金</span><span class="rv" :class="depositColor(r.depositStatus)">{{ r.depositAmount ? '¥'+r.depositAmount.toLocaleString() : '¥500' }} · {{ depositLabel(r.depositStatus) }}</span></div>
        </div>

        <div v-if="r.status === 'CONFIRMED' || r.status === 'PENDING_DEPOSIT'" class="bk-actions">
          <button v-if="r.status === 'CONFIRMED'" class="ac-btn ac-soft" @click="confirmReschedule(r)">改期</button>
          <button v-if="r.depositStatus !== 'CUSTOMER_PAID' && r.depositStatus !== 'PAID'" class="ac-btn ac-danger" @click="confirmCancel(r)">取消</button>
        </div>
      </div>
    </div>

    <!-- ═══ 底部操作（空状态时显示） ═══ -->
    <div v-if="searched && reservations.length === 0" class="mb-bottom">
      <button class="mb-start" @click="goBooking">
        <van-icon name="calendar-o" size="18" style="margin-right:6px;" />
        开始预约
      </button>
    </div>

    <!-- 取消弹窗 -->
    <van-dialog v-model:show="showCancelDialog" title="取消预约" :show-cancel-button="true"
      confirm-button-color="var(--pink-500)" @confirm="doCancel">
      <p style="padding:0 20px;font-size:14px;color:var(--ink-700);line-height:1.6;">
        预约取消后定金不予退还。如需退款请联系店主微信。
      </p>
    </van-dialog>

    <!-- 改期日历 -->
    <van-calendar v-model:show="showRescheduleCalendar" :min-date="minDate" :max-date="maxDate"
      color="var(--pink-500)" @confirm="onCalendarConfirm" />
    <!-- 改期时段 -->
    <van-action-sheet v-model:show="showSlotSheet" title="选择新时间">
      <div style="padding:16px;">
        <van-loading v-if="loadingSlots" size="20px" vertical />
        <div v-else-if="availableSlots.length === 0" style="text-align:center;color:var(--ink-500);padding:24px;">暂无可用时段</div>
        <div v-else v-for="slot in availableSlots" :key="slot.timeSlot"
          :class="['slot-opt', { disabled: !slot.available }]"
          @click="slot.available && (selectedSlot = slot.timeSlot, showSlotSheet = false, showRescheduleConfirm = true)">
          <span>{{ slot.timeSlot }}</span>
          <van-tag :type="slot.available ? 'success' : 'default'" size="small" round>{{ slot.available ? '可选' : '已满' }}</van-tag>
        </div>
      </div>
    </van-action-sheet>
    <van-dialog v-model:show="showRescheduleConfirm" title="改期确认" :show-cancel-button="true"
      confirm-button-color="var(--pink-500)" @confirm="doReschedule">
      <p style="padding:0 20px;font-size:14px;color:var(--ink-700);">确定要更改预约时间吗？</p>
      <p v-if="selectedDate && selectedSlot" style="text-align:center;font-weight:700;color:var(--pink-500);">{{ formatDate(selectedDate) }} {{ selectedSlot }}</p>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { getReservationsByContact, userCancelReservation, rescheduleReservation, getAvailability } from '../api';

const router = useRouter();
const goBack = () => router.back();
const goBooking = () => router.push('/menu');

const contactId = ref('');
const searching = ref(false);
const searched = ref(false);
const reservations = ref([]);
const showSearch = ref(true);

const showCancelDialog = ref(false);
const cancellingReservation = ref(null);

const reschedulingReservation = ref(null);
const showRescheduleCalendar = ref(false);
const showSlotSheet = ref(false);
const showRescheduleConfirm = ref(false);
const selectedDate = ref('');
const selectedSlot = ref('');
const availableSlots = ref([]);
const loadingSlots = ref(false);

onMounted(() => {
  const stored = sessionStorage.getItem('userContactId');
  if (stored) {
    contactId.value = stored;
    showSearch.value = false;
    searchReservations();
  }
});

const minDate = computed(() => new Date());
const maxDate = computed(() => { const d = new Date(); d.setDate(d.getDate() + 14); return d; });

const formatDate = (d) => d ? `${parseInt(d.split('-')[1])}月${parseInt(d.split('-')[2])}日` : '';
const toDateStr = (d) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;

const searchReservations = async () => {
  if (!contactId.value.trim()) { showToast('请输入联系方式'); return; }
  searching.value = true; searched.value = true;
  try {
    const res = await getReservationsByContact(contactId.value.trim());
    if (res.code === 200) reservations.value = res.data;
    else showToast(res.message || '查询失败');
  } catch { showToast('查询失败'); }
  finally { searching.value = false; }
};

const statusBadgeClass = (s) => {
  if (s === 'CONFIRMED') return 'confirmed';
  if (s === 'COMPLETED') return 'completed';
  if (s === 'CANCELLED') return 'cancelled';
  return 'pending';
};
const statusLabel = (s) => ({ PENDING_DEPOSIT: '待确认', CONFIRMED: '已确认', COMPLETED: '已完成', CANCELLED: '已取消' })[s] || s;
const depositLabel = (s) => ({ NONE: '未付', CUSTOMER_PAID: '待确认', PAID: '已付', REFUNDED: '已退', FORFEITED: '已没收' })[s] || s;
const depositColor = (s) => ({ PAID: 'c-green', CUSTOMER_PAID: 'c-yellow', REFUNDED: 'c-gray' })[s] || '';

const confirmCancel = (r) => { cancellingReservation.value = r; showCancelDialog.value = true; };
const doCancel = async () => {
  if (!cancellingReservation.value) return;
  try {
    const res = await userCancelReservation(cancellingReservation.value.id);
    if (res.code === 200) { showToast('已取消'); await searchReservations(); }
    else showToast(res.message || '取消失败');
  } catch { showToast('取消失败'); }
  cancellingReservation.value = null;
};

const confirmReschedule = (r) => { reschedulingReservation.value = r; showRescheduleCalendar.value = true; };
const onCalendarConfirm = async (date) => {
  showRescheduleCalendar.value = false;
  selectedDate.value = toDateStr(date);
  loadingSlots.value = true; showSlotSheet.value = true;
  try {
    const res = await getAvailability(selectedDate.value);
    availableSlots.value = res.data || [];
  } catch { showToast('加载失败'); showSlotSheet.value = false; }
  finally { loadingSlots.value = false; }
};
const doReschedule = async () => {
  if (!reschedulingReservation.value || !selectedDate.value || !selectedSlot.value) return;
  try {
    const res = await rescheduleReservation(reschedulingReservation.value.id, selectedDate.value, selectedSlot.value);
    if (res.code === 200) { showToast('改期成功'); await searchReservations(); }
    else showToast(res.message || '改期失败');
  } catch { showToast('改期失败'); }
  reschedulingReservation.value = null;
  selectedDate.value = ''; selectedSlot.value = '';
};
</script>

<style scoped>
.my-booking { min-height: 100vh; background: transparent; padding-bottom: 100px; }

/* Nav */
.mb-nav {
  position: sticky; top: 0; z-index: 20;
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  background: rgba(253,243,245,0.78);
  border-bottom: 1px solid rgba(232,127,160,0.16);
  padding: 12px 14px; display: flex; align-items: center; gap: 10px;
}
.mb-back {
  width: 36px; height: 36px; border-radius: 999px;
  background: rgba(255,255,255,0.85); border: 1px solid var(--pink-200);
  display: grid; place-items: center; cursor: pointer;
  box-shadow: var(--shadow-1); flex-shrink: 0;
}
.mb-back:active { transform: scale(0.95); }
.mb-title { flex:1; text-align:center; font-family:var(--font-cjk); font-weight:800; color:var(--ink-900); font-size:15.5px; }

/* Search */
.search-area { padding: 14px 14px 0; }
.search-box {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px 8px 14px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
}
.search-input { flex:1; border:0; outline:0; font-family:var(--font-cjk); font-size:14px; color:var(--ink-900); background:transparent; }
.search-btn {
  padding: 6px 14px; border-radius: 999px; border:0;
  background: var(--pink-500); color:#fff; font-family:var(--font-body);
  font-size:13px; font-weight:600; cursor:pointer;
}

/* Switch */
.switch-row { display:flex; justify-content:flex-end; padding:8px 14px 0; }
.switch-btn { background:transparent; border:0; font-size:12px; color:var(--pink-600); cursor:pointer; font-family:var(--font-cjk); }

/* Empty */
.empty-state { display:flex; flex-direction:column; align-items:center; margin-top:100px; }
.empty-icon { font-size:56px; margin-bottom:12px; }
.empty-title { font-family:var(--font-cjk); font-weight:700; font-size:20px; color:var(--ink-900); }
.empty-sub { font-family:var(--font-cjk); font-size:14px; color:var(--ink-500); margin-top:4px; }

/* List */
.booking-list { padding: 14px; display:flex; flex-direction:column; gap:12px; }
.bk-card {
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
  overflow: hidden;
}
.bk-card.muted { opacity:0.6; }
.bk-header {
  display:flex; justify-content:space-between; align-items:center;
  padding:14px 16px 8px;
}
.bk-date { font-family:var(--font-cjk); font-weight:600; font-size:15px; color:var(--ink-900); }
.bk-time { font-family:var(--font-cjk); font-size:13px; color:var(--ink-500); margin-left:8px; }
.bk-reminder { margin:0 16px 8px; padding:6px 10px; background:#fef6e0; border-radius:var(--radius-sm); font-size:12px; color:#b8860b; }
.bk-body { padding:0 16px 8px; }
.bk-row { display:flex; justify-content:space-between; padding:4px 0; font-size:13px; }
.rk { color:var(--ink-500); font-family:var(--font-cjk); }
.rv { color:var(--ink-700); font-family:var(--font-cjk); }
.c-green { color:var(--accent-green) !important; }
.c-yellow { color:#b8860b !important; }
.c-gray { color:var(--ink-400) !important; }
.bk-actions { display:flex; gap:8px; padding:8px 16px 14px; }
.ac-btn {
  padding:6px 16px; border-radius:999px; border:1px solid var(--border-soft);
  font-family:var(--font-cjk); font-size:12px; font-weight:600; cursor:pointer;
  transition:all var(--dur-base) var(--ease-soft);
}
.ac-soft { background:var(--pink-50); color:var(--pink-600); border-color:var(--pink-200); }
.ac-danger { background:#fff; color:var(--accent-red); }
.ac-btn:active { transform:scale(0.95); }

/* Bottom CTA */
.mb-bottom {
  position:fixed; bottom:0; left:0; right:0;
  padding:12px 14px 24px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-top: 1px solid var(--border-soft);
  z-index:100;
}
.mb-start {
  width:100%; padding:14px;
  border-radius:999px; border:0;
  background: linear-gradient(135deg, #ff6b9d 0%, #e8436e 100%);
  color:#fff; font-family:var(--font-cjk);
  font-size:16px; font-weight:700;
  cursor:pointer; display:flex; align-items:center; justify-content:center;
  box-shadow:0 4px 16px rgba(232,67,110,0.35);
}
.mb-start:active { transform:scale(0.97); }
@media (min-width:768px) { .mb-bottom { width:390px; left:calc(100vw - 390px); right:auto; } }

/* Slot options */
.slot-opt { display:flex; justify-content:space-between; align-items:center; padding:12px 0; border-bottom:1px solid var(--border-soft); cursor:pointer; font-family:var(--font-cjk); }
.slot-opt.disabled { opacity:0.4; cursor:not-allowed; }
</style>
