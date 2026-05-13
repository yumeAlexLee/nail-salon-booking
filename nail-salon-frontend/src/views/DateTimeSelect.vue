<template>
  <div class="date-time-select">
    <!-- ═══ Nav ═══ -->
    <div class="dt-nav">
      <button class="dt-back" @click="goBack">
        <van-icon name="arrow-left" size="18" color="var(--ink-700)" />
      </button>
      <div class="dt-title">选择时间</div>
      <div style="width:36px;"></div>
    </div>

    <!-- ═══ 已选服务卡片 ═══ -->
    <div v-if="selectedService" class="svc-card">
      <div class="svc-thumb">
        <img :src="'/' + thumbFile(selectedService)" class="svc-img" />
      </div>
      <div class="svc-info">
        <div class="svc-name">{{ selectedService.name }}</div>
        <div class="svc-meta">¥{{ selectedService.price.toLocaleString() }} · 约 {{ selectedService.duration }} 分</div>
      </div>
      <button class="svc-change" @click="changeService">更换</button>
    </div>

    <!-- ═══ 日期行 ═══ -->
    <div class="date-row-label">选择日期</div>
    <div class="date-row">
      <button
        v-for="d in dateList"
        :key="d.iso"
        :class="['date-cell', {
          'date-active': selectedDate === d.iso,
          'date-closed': d.closed,
          'date-today': d.isToday,
        }]"
        @click="selectDate(d)"
      >
        <span class="date-wday">{{ d.weekday }}</span>
        <span class="date-num">{{ d.day }}</span>
        <span v-if="d.closed" class="date-rest">休</span>
        <span v-if="d.isToday && !d.closed" class="date-dot"></span>
      </button>
    </div>

    <!-- ═══ 时段 ═══ -->
    <div v-if="selectedDate" class="slot-section">
      <div class="slot-label">{{ formattedDate }} 的可用时段</div>
      <van-loading v-if="loading" size="20px" vertical style="margin:30px 0;">加载中...</van-loading>
      <div v-else class="slot-grid">
        <button
          v-for="slot in slots"
          :key="slot.timeSlot"
          :class="['slot-btn', {
            'slot-off': !slot.available,
            'slot-on': slot.available && selectedSlot !== slot.timeSlot,
            'slot-chosen': selectedSlot === slot.timeSlot,
          }]"
          :disabled="!slot.available"
          @click="selectSlot(slot)"
        >
          <span v-if="!slot.available" class="slot-line">{{ slot.timeSlot }}</span>
          <span v-else>{{ slot.timeSlot }}</span>
        </button>
      </div>
    </div>

    <!-- ═══ 底部操作栏 ═══ -->
    <div class="dt-bottom">
      <button
        class="dt-next"
        :class="{ disabled: !selectedSlot }"
        :disabled="!selectedSlot"
        @click="goToForm"
      >
        下一步 · 填写信息
        <van-icon name="arrow" size="16" style="margin-left:4px;" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { getAvailability } from '../api';

const router = useRouter();
const goBack = () => router.back();

// 已选服务
const selectedService = JSON.parse(sessionStorage.getItem('selectedService') || 'null');

const thumbFile = (svc) => {
  const map = {
    '换款卸甲·本甲': 'work4.jpg', '他店来·本甲': 'work8.jpg', '他店来·甲片': 'work11.jpg',
    '单色': 'work8.jpg', '裸色': 'work4.jpg', '简约·猫眼': 'work1.jpg', '轻奢·法式': 'work6.jpg',
    '高位半贴': 'work11.jpg', '塑形半贴': 'work15.jpg', '加长浅贴': 'work12.jpg',
    '叠加层次款式': 'work9.jpg', '人物手绘': 'work17.jpg', '手工造型': 'work5.jpg',
  };
  return map[svc?.name] || 'work1.jpg';
};

const changeService = () => {
  router.back();
};

// ─── 日期 ───
const weekdayJP = ['日', '月', '火', '水', '木', '金', '土'];
const today = new Date();

const dateList = computed(() => {
  const list = [];
  for (let i = 0; i < 14; i++) {
    const d = new Date(today);
    d.setDate(today.getDate() + i);
    const iso = d.toISOString().slice(0, 10);
    const day = d.getDay();
    const isMonday = day === 1;
    list.push({
      iso,
      day: d.getDate(),
      weekday: weekdayJP[day],
      isToday: i === 0,
      closed: isMonday, // 定休日：周一
    });
  }
  return list;
});

const selectedDate = ref('');
const formattedDate = ref('');
const selectedSlot = ref('');
const slots = ref([]);
const loading = ref(false);

const selectDate = async (d) => {
  if (d.closed) return;
  selectedDate.value = d.iso;
  selectedSlot.value = '';
  
  // 格式化：5月14日（水）
  const dt = new Date(d.iso + 'T00:00:00');
  formattedDate.value = `${dt.getMonth() + 1}月${dt.getDate()}日（${d.weekday}）`;

  loading.value = true;
  try {
    const res = await getAvailability(d.iso);
    if (res.code === 200) slots.value = res.data;
  } catch (e) {
    console.error('获取时间失败', e);
  } finally {
    loading.value = false;
  }
};

const selectSlot = (slot) => {
  if (!slot.available) return;
  selectedSlot.value = slot.timeSlot;
};

const goToForm = () => {
  if (!selectedSlot.value) return;
  sessionStorage.setItem('reserveDate', selectedDate.value);
  sessionStorage.setItem('timeSlot', selectedSlot.value);
  router.push('/form');
};
</script>

<style scoped>
.date-time-select {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 80px;
}

/* ─── Nav ─── */
.dt-nav {
  position: sticky; top: 0; z-index: 20;
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  background: rgba(253,243,245,0.78);
  border-bottom: 1px solid rgba(232,127,160,0.16);
  padding: 12px 14px;
  display: flex; align-items: center; gap: 10px;
}
.dt-back {
  width: 36px; height: 36px; border-radius: 999px;
  background: rgba(255,255,255,0.85);
  border: 1px solid var(--pink-200);
  display: grid; place-items: center; cursor: pointer;
  box-shadow: var(--shadow-1); flex-shrink: 0;
}
.dt-back:active { transform: scale(0.95); }
.dt-title {
  flex: 1; text-align: center;
  font-family: var(--font-cjk); font-weight: 800;
  color: var(--ink-900); font-size: 15.5px;
}

/* ─── Service card ─── */
.svc-card {
  margin: 14px 14px 0;
  padding: 12px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
  display: flex;
  align-items: center;
  gap: 12px;
}
.svc-thumb {
  width: 56px; height: 56px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--pink-100);
}
.svc-img { width: 100%; height: 100%; object-fit: cover; }
.svc-info { flex: 1; min-width: 0; }
.svc-name {
  font-family: var(--font-cjk); font-weight: 700;
  font-size: 14px; color: var(--ink-900);
}
.svc-meta {
  font-family: var(--font-body);
  font-size: 12px; color: var(--ink-500);
  margin-top: 2px;
}
.svc-change {
  font-family: var(--font-body);
  font-size: 12px; font-weight: 700;
  color: var(--accent-red);
  background: transparent; border: 0;
  cursor: pointer; flex-shrink: 0; padding: 4px 6px;
}

/* ─── Date row ─── */
.date-row-label {
  font-family: var(--font-cjk); font-weight: 700;
  font-size: 15px; color: var(--ink-900);
  padding: 14px 14px 8px;
}
.date-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  padding: 0 14px;
}
.date-cell {
  width: 100%;
  height: 68px;
  border-radius: var(--radius-md);
  background: var(--surface-frosted-strong);
  border: 1px solid var(--border-soft);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 2px;
  cursor: pointer;
  position: relative;
  transition: all 220ms cubic-bezier(0.22,0.61,0.36,1);
}
.date-cell:active { transform: scale(0.95); }
.date-wday {
  font-family: var(--font-body);
  font-size: 12px; font-weight: 500;
  color: var(--ink-500);
}
.date-num {
  font-family: var(--font-body);
  font-size: 18px; font-weight: 700;
  color: var(--ink-900);
}
/* today dot */
.date-dot {
  position: absolute;
  top: 6px; right: 6px;
  width: 6px; height: 6px;
  border-radius: 50%;
  background: var(--pink-500);
}
/* closed */
.date-closed {
  background: #f0eeec !important;
  border-color: #e0ddda !important;
  cursor: default;
}
.date-closed .date-wday,
.date-closed .date-num {
  color: #bbb !important;
}
.date-rest {
  font-family: var(--font-cjk);
  font-size: 10px; font-weight: 700;
  color: var(--ink-400);
  margin-top: -1px;
}
/* selected */
.date-active {
  background: var(--pink-500) !important;
  border-color: var(--pink-500) !important;
  box-shadow: var(--shadow-2);
}
.date-active .date-wday,
.date-active .date-num {
  color: #fff !important;
}

/* ─── Slots ─── */
.slot-section {
  padding: 0 14px;
  margin-top: 16px;
}
.slot-label {
  font-family: var(--font-cjk); font-weight: 700;
  font-size: 15px; color: var(--ink-900);
  margin-bottom: 12px;
}
.slot-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.slot-btn {
  padding: 14px 0;
  border-radius: var(--radius-md);
  font-family: var(--font-body);
  font-size: 15px; font-weight: 600;
  border: 1px solid var(--border-soft);
  cursor: pointer;
  transition: all var(--dur-base) var(--ease-soft);
  text-align: center;
}
.slot-on {
  background: var(--surface-card);
  color: var(--ink-900);
  box-shadow: var(--shadow-1);
}
.slot-on:active {
  transform: scale(0.96);
  background: var(--pink-50);
}
.slot-off {
  background: #f5f3f0;
  color: #c4bdb0;
  border-color: transparent;
  cursor: not-allowed;
}
.slot-line {
  text-decoration: line-through;
}
.slot-chosen {
  background: var(--pink-500);
  color: #fff;
  border-color: var(--pink-500);
  box-shadow: var(--shadow-2);
}

/* ─── Bottom bar ─── */
.dt-bottom {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 14px 24px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-top: 1px solid var(--border-soft);
  z-index: 100;
}
@media (min-width: 768px) {
  .dt-bottom {
    width: 390px;
    left: calc(100vw - 390px);
    right: auto;
  }
}
.dt-next {
  width: 100%;
  padding: 15px 26px;
  border-radius: 999px;
  border: 0;
  background: linear-gradient(135deg, #ff6b9d 0%, #e8436e 100%);
  color: #fff;
  font-family: var(--font-cjk);
  font-size: 16px; font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(232,67,110,0.35);
  transition: all var(--dur-base) var(--ease-soft);
}
.dt-next:active {
  transform: scale(0.97);
}
.dt-next.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
