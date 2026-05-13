<template>
  <div class="date-time-select">
    <van-nav-bar
      :title="$t('nav.selectTime')"
      left-arrow
      @click-left="goBack"
      class="glass-effect"
      fixed placeholder
      :border="false"
    />
    
    <div class="calendar-wrapper">
      <van-calendar
        title=""
        :poppable="false"
        :show-confirm="false"
        :min-date="minDate"
        :max-date="maxDate"
        :formatter="dayFormatter"
        :row-height="76"
        @confirm="onDateSelect"
      />
    </div>

    <div class="slots-area" v-if="selectedDate">
      <div class="slots-container">
        <h3 class="slots-title">{{ formattedDate }} {{ $t('date.availableSlots') }}</h3>
        
        <van-loading v-if="loading" size="24px" vertical>{{ $t('date.loading') }}</van-loading>
        
        <van-grid :column-num="2" :gutter="12" v-else>
          <van-grid-item 
            v-for="slot in slots" 
            :key="slot.timeSlot"
            :class="['slot-item', { 'slot-disabled': !slot.available, 'slot-active': selectedSlot === slot.timeSlot }]"
            @click="selectSlot(slot)"
          >
            <div class="slot-content apple-btn-active">
              <div class="slot-text">{{ slot.timeSlot }}</div>
              <div class="slot-status">{{ slot.available ? $t('date.available') : $t('date.full') }}</div>
            </div>
          </van-grid-item>
        </van-grid>
      </div>
      
      <div class="bottom-btn glass-effect">
        <van-button type="primary" block round size="large" @click="goToForm" class="apple-btn-active">
          {{ $t('date.next') }}
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getAvailability } from '../api';
import { Toast } from 'vant';
import holiday_jp from '@holiday-jp/holiday_jp';

const router = useRouter();
const minDate = new Date();
const maxDate = new Date();
maxDate.setDate(maxDate.getDate() + 14); // 开放未来14天

const selectedDate = ref('');
const formattedDate = ref('');
const selectedSlot = ref('');
const slots = ref([]);
const loading = ref(false);

const goBack = () => router.back();

// 日历格式化，展示日本节假日
const dayFormatter = (day) => {
  const isHoliday = holiday_jp.isHoliday(day.date);
  if (isHoliday) {
    let holidayName = holiday_jp.between(day.date, day.date)[0].name;
    // 简化超长的振替休日名称
    if (holidayName.includes('振替休日')) {
      holidayName = '振替休日';
    }
    day.bottomInfo = holidayName;
    day.className = 'holiday-text';
  }
  return day;
};

const formatDate = (date) => {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
};

const onDateSelect = async (date) => {
  const dateStr = formatDate(date);
  selectedDate.value = dateStr;
  formattedDate.value = `${date.getMonth() + 1}月${date.getDate()}日`;
  selectedSlot.value = '';
  
  loading.value = true;
  try {
    const res = await getAvailability(dateStr);
    if (res.code === 200) slots.value = res.data;
  } catch (error) {
    console.error('获取时间失败', error);
  } finally {
    loading.value = false;
  }
};

const selectSlot = (slot) => {
  if (!slot.available) return;
  selectedSlot.value = slot.timeSlot;
};

const goToForm = () => {
  sessionStorage.setItem('reserveDate', selectedDate.value);
  sessionStorage.setItem('timeSlot', selectedSlot.value);
  router.push('/form');
};
</script>

<style scoped>
.date-time-select {
  min-height: 100vh;
  background: var(--surface-bg);
  display: flex;
  flex-direction: column;
}

/* ─── Calendar card: frosted glass ─── */
.calendar-wrapper {
  margin: 16px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-2);
  flex-shrink: 0;
}
:deep(.van-calendar__header) {
  box-shadow: none;
}

/* ─── 插槽区域：flex 撑满，把按钮推到底部 ─── */
.slots-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.slots-container {
  flex: 1;
  padding: 0 16px;
}
.slots-title {
  margin: 24px 0 16px 4px;
  font-size: var(--fs-18);
  color: var(--fg-1);
  font-weight: 600;
  font-family: var(--font-cjk);
}
:deep(.slot-item .van-grid-item__content) {
  padding: 0;
  border-radius: var(--radius-md);
  overflow: hidden;
  background-color: transparent;
}
.slot-content {
  width: 100%;
  padding: 16px 0;
  background: var(--surface-frosted);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-1);
  transition: all var(--dur-base) var(--ease-soft);
}
.slot-text {
  font-size: 17px;
  color: var(--fg-1);
  font-weight: 600;
}
.slot-status {
  font-size: var(--fs-13);
  color: var(--fg-2);
  margin-top: 4px;
}
.slot-disabled .slot-content {
  background: var(--pink-50);
  opacity: 0.6;
  border: none;
  box-shadow: none;
}
.slot-active .slot-content {
  background: var(--pink-500);
  border-color: var(--pink-500);
  box-shadow: var(--shadow-2);
}
.slot-active .slot-text,
.slot-active .slot-status {
  color: var(--fg-on-pink);
}

/* ─── 底部按钮：sticky 吸附，不受 transform 影响 ─── */
.bottom-btn {
  position: sticky;
  bottom: 0;
  padding: 16px 24px 32px 24px;
  margin-top: 12px;
  border-top: 1px solid var(--border-soft);
  z-index: 100;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}
.bottom-btn .van-button {
  font-weight: 600;
}

/* ─── 节假日文字 ─── */
:deep(.van-calendar__bottom-info) {
  white-space: normal;
  word-break: break-all;
  width: 100%;
  font-size: 10px;
  line-height: 1.2;
  margin-top: 2px;
}
:deep(.holiday-text) .van-calendar__bottom-info {
  color: var(--fg-3);
  font-weight: 500;
  transform: scale(0.9);
}
:deep(.van-calendar__day--selected .van-calendar__bottom-info) {
  color: var(--fg-on-pink) !important;
}
:deep(.van-calendar__day--selected) {
  border-radius: 12px;
}
</style>
