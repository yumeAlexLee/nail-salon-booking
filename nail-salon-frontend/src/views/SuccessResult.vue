<template>
  <div class="success-result">
    <!-- ═══ 头部情感区 ═══ -->
    <div class="sr-header">
      <div class="sr-emoji">🌷</div>
      <div class="sr-seeyou">See you soon</div>
      <div class="sr-status">预约已提交，等待店主确认</div>
      <div class="sr-order">编号 {{ orderId }}</div>
    </div>

    <!-- ═══ 订单快照 ═══ -->
    <div class="snapshot-card" v-if="selectedService">
      <div class="snap-left">
        <div class="snap-name">{{ selectedService.name }}</div>
        <div class="snap-time">{{ reserveDate }} {{ timeSlot }}</div>
      </div>
      <span class="snap-badge">待确认</span>
    </div>

    <!-- ═══ 定金支付模块 ═══ -->
    <div class="pay-section" v-if="!paid">
      <div class="pay-amount-row">
        <span class="pay-label">支付定金</span>
        <span class="pay-amount">¥{{ depositAmount.toLocaleString() }}</span>
      </div>

      <!-- 支付方式 Tabs -->
      <div class="pay-tabs">
        <button
          :class="['pay-tab', { active: payMethod === 'wechat' }]"
          @click="payMethod = 'wechat'"
        >💚 微信支付</button>
        <button
          :class="['pay-tab', { active: payMethod === 'alipay' }]"
          @click="payMethod = 'alipay'"
        >🔵 支付宝</button>
      </div>

      <!-- 二维码 -->
      <div class="qr-area" @click="showQR">
        <img :src="qrSrc" class="qr-img" alt="支付二维码" />
      </div>
      <div class="qr-hint">扫码完成后点击下方按钮</div>

      <!-- 我已付款 -->
      <button class="pay-btn" @click="handleClaimPaid" :disabled="claiming">
        <van-loading v-if="claiming" size="16" color="#fff" style="margin-right:6px;" />
        我已付款 · ¥{{ depositAmount.toLocaleString() }}
      </button>
    </div>

    <!-- ═══ 已付款状态 ═══ -->
    <div v-else class="paid-state">
      <div class="paid-icon">⏳</div>
      <div class="paid-title">已提交确认</div>
      <div class="paid-desc">店主确认到账后预约将正式生效</div>
    </div>

    <!-- ═══ 底部导航 ═══ -->
    <div class="sr-nav">
      <button class="sr-nav-btn" @click="showGuidePreview">
        <van-icon name="location-o" size="22" />
        <span>店铺指引</span>
      </button>
      <button class="sr-nav-btn" @click="goHome">
        <van-icon name="home-o" size="22" />
        <span>回主页</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { showImagePreview, showToast } from 'vant';
import { claimPaid } from '../api';

import wechatQr from '../assets/wechat-qr.png';
import alipayQr from '../assets/alipay-qr.png';

const router = useRouter();

// 订单数据
const selectedService = JSON.parse(sessionStorage.getItem('selectedService') || 'null');
const reserveDate = sessionStorage.getItem('reserveDate') || '';
const timeSlot = sessionStorage.getItem('timeSlot') || '';
const isNew = sessionStorage.getItem('customerType') === 'NEW';

// 订单号
const orderId = 'B-' + String(Date.now()).slice(-8);

// 定金
const depositAmount = computed(() => {
  const stored = sessionStorage.getItem('depositAmount');
  if (stored) return parseInt(stored);
  if (!selectedService) return 500;
  const price = isNew ? Math.round(selectedService.price * 0.8) : selectedService.price;
  return Math.ceil(price * 0.3);
});

// 支付
const payMethod = ref('wechat');
const claiming = ref(false);
const paid = ref(false);

const qrSrc = computed(() => payMethod.value === 'wechat' ? wechatQr : alipayQr);

const showQR = () => {
  showImagePreview({ images: [qrSrc.value], closeable: true });
};

const handleClaimPaid = async () => {
  claiming.value = true;
  try {
    const id = sessionStorage.getItem('lastReservationId');
    if (id) await claimPaid(id);
    paid.value = true;
    showToast('已提交确认');
  } catch {
    showToast('提交失败，请重试');
  } finally {
    claiming.value = false;
  }
};

onMounted(() => {
  // 检查是否已付款
  // (后端已有 claimPaid API)
});

const showGuidePreview = () => router.push('/guide');
const goHome = () => {
  sessionStorage.removeItem('reserveDate');
  sessionStorage.removeItem('timeSlot');
  sessionStorage.removeItem('lastReservationId');
  sessionStorage.removeItem('depositAmount');
  router.replace('/');
};
</script>

<style scoped>
.success-result {
  padding: 32px 18px 100px;
  background: transparent;
  min-height: 100vh;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* ─── Header ─── */
.sr-header {
  text-align: center;
  margin-bottom: 16px;
}
.sr-emoji { font-size: 40px; margin-bottom: 6px; }
.sr-seeyou {
  font-family: Caveat Brush, cursive;
  font-size: 28px;
  color: var(--pink-500);
  letter-spacing: 0.04em;
  transform: rotate(-1deg);
}
.sr-status {
  font-family: var(--font-cjk);
  font-size: 14px;
  color: var(--ink-700);
  margin-top: 4px;
}
.sr-order {
  font-family: var(--font-body);
  font-size: 11px;
  color: var(--ink-400);
  margin-top: 2px;
  letter-spacing: 0.02em;
}

/* ─── Snapshot ─── */
.snapshot-card {
  width: 100%;
  max-width: 380px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
  margin-bottom: 20px;
}
.snap-left { flex: 1; }
.snap-name {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 14px;
  color: var(--ink-900);
}
.snap-time {
  font-family: var(--font-cjk);
  font-size: 12px;
  color: var(--ink-500);
  margin-top: 2px;
}
.snap-badge {
  font-family: var(--font-body);
  font-weight: 700;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fef6e0;
  color: #b8860b;
  flex-shrink: 0;
}

/* ─── Payment section ─── */
.pay-section {
  width: 100%;
  max-width: 380px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2);
  padding: 20px;
  margin-bottom: 20px;
}
.pay-amount-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-soft);
}
.pay-label {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 15px;
  color: var(--ink-700);
}
.pay-amount {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 24px;
  color: var(--pink-500);
}

/* Tabs */
.pay-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}
.pay-tab {
  flex: 1;
  padding: 10px 0;
  border-radius: 999px;
  border: 1px solid var(--pink-200);
  background: var(--pink-50);
  font-family: var(--font-cjk);
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-700);
  cursor: pointer;
  transition: all 220ms cubic-bezier(0.22,0.61,0.36,1);
}
.pay-tab.active {
  background: var(--pink-500);
  color: #fff;
  border-color: var(--pink-500);
  box-shadow: var(--shadow-2);
}

/* QR */
.qr-area {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
  cursor: pointer;
}
.qr-img {
  width: 180px;
  height: 180px;
  border-radius: var(--radius-md);
  object-fit: contain;
  background: #fff;
  padding: 8px;
  box-shadow: var(--shadow-1);
}
.qr-hint {
  text-align: center;
  font-family: var(--font-cjk);
  font-size: 12px;
  color: var(--ink-400);
  margin-bottom: 14px;
}

/* Pay button */
.pay-btn {
  width: 100%;
  padding: 14px;
  border-radius: 999px;
  border: 0;
  background: linear-gradient(135deg, #ff6b9d 0%, #e8436e 100%);
  color: #fff;
  font-family: var(--font-cjk);
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 16px rgba(232,67,110,0.35);
  transition: all var(--dur-base) var(--ease-soft);
}
.pay-btn:active { transform: scale(0.97); }
.pay-btn:disabled { opacity: 0.6; cursor: not-allowed; }

/* ─── Paid state ─── */
.paid-state {
  width: 100%;
  max-width: 380px;
  text-align: center;
  padding: 30px 20px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2);
  margin-bottom: 20px;
}
.paid-icon { font-size: 48px; margin-bottom: 8px; }
.paid-title {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 18px;
  color: var(--ink-900);
}
.paid-desc {
  font-family: var(--font-cjk);
  font-size: 13px;
  color: var(--ink-500);
  margin-top: 4px;
}

/* ─── Bottom nav ─── */
.sr-nav {
  display: flex;
  gap: 16px;
  margin-top: auto;
}
.sr-nav-btn {
  width: 100px;
  height: 100px;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: var(--shadow-1);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  color: var(--ink-700);
  font-family: var(--font-cjk);
  font-size: 12px;
  font-weight: 600;
  transition: all var(--dur-base) var(--ease-soft);
}
.sr-nav-btn:active {
  transform: scale(0.95);
  background: var(--pink-50);
}
</style>
