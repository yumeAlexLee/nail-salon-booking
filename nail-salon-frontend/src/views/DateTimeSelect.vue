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
        style="height: 420px;"
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
  background-color: var(--apple-bg);
  display: flex;
  flex-direction: column;
}
.calendar-wrapper {
  margin: 16px;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  background: var(--apple-white);
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
  font-size: 18px;
  color: var(--apple-text);
  font-weight: 600;
}
:deep(.slot-item .van-grid-item__content) {
  padding: 0;
  border-radius: 14px;
  overflow: hidden;
  background-color: transparent;
}
.slot-content {
  width: 100%;
  padding: 16px 0;
  background-color: var(--apple-white);
  border: 1px solid rgba(0,0,0,0.05);
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
}
.slot-text {
  font-size: 17px;
  color: var(--apple-text);
  font-weight: 600;
}
.slot-status {
  font-size: 13px;
  color: var(--apple-text-secondary);
  margin-top: 4px;
}
.slot-disabled .slot-content {
  background-color: #f5f5f7;
  opacity: 0.6;
  border: none;
  box-shadow: none;
}
.slot-active .slot-content {
  background-color: var(--apple-black);
  border-color: var(--apple-black);
}
.slot-active .slot-text,
.slot-active .slot-status {
  color: var(--apple-white);
}

/* ─── 底部按钮：sticky 吸附，不受 transform 影响 ─── */
.bottom-btn {
  position: sticky;
  bottom: 0;
  padding: 16px 24px 32px 24px;
  margin-top: 12px;
  border-top: 1px solid rgba(0,0,0,0.05);
  z-index: 100;
}
.bottom-btn .van-button {
  font-weight: 600;
}
:deep(.van-calendar__bottom-info) {
  white-space: normal;
  word-break: break-all;
  width: 100%;
  font-size: 10px;
  line-height: 1.2;
  margin-top: 2px;
}
:deep(.holiday-text) .van-calendar__bottom-info {
  color: var(--apple-text-secondary);
  font-weight: 500;
  transform: scale(0.9);
}
:deep(.van-calendar__day--selected .van-calendar__bottom-info) {
  color: var(--apple-white) !important;
}
:deep(.van-calendar__day--selected) {
  border-radius: 12px;
}
</style>
