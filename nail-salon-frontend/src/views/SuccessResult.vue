<template>
  <div class="success-result">
    <!-- 成功动画 -->
    <div class="result-icon">
      <van-icon name="checked" color="var(--accent-green)" size="80" />
    </div>
    <h2 class="title">{{ $t('success.title') }}</h2>
    <p class="desc">{{ $t('success.desc') }}</p>

    <!-- ─── 定金支付模块 ─────────────────────────────── -->
    <div class="payment-card" v-if="depositAmount > 0">
      <!-- 未支付状态 -->
      <template v-if="depositStatus !== 'CUSTOMER_PAID'">
        <div class="payment-header">
          <span class="payment-label">💰 预付定金</span>
          <span class="payment-amount">¥{{ depositAmount.toLocaleString() }}</span>
        </div>
        <p class="payment-desc">请使用微信或支付宝扫码支付¥{{ depositAmount.toLocaleString() }}。预约取消定金恕不退还。</p>

        <div class="payment-methods">
          <div class="method-item" @click="handlePay('wechat')">
            <span class="method-icon">💚</span>
            <span class="method-name">微信支付</span>
          </div>
          <div class="method-item" @click="handlePay('alipay')">
            <span class="method-icon">🔵</span>
            <span class="method-name">支付宝</span>
          </div>
        </div>

        <van-button block round type="primary" @click="handleClaimPaid" :loading="claiming" style="margin-top:12px">
          我已付款
        </van-button>
      </template>

      <!-- 已付款待确认状态 -->
      <template v-else-if="depositStatus === 'CUSTOMER_PAID'">
        <div class="customer-paid-section">
          <div class="paid-animation">⏳</div>
          <div class="paid-title">已提交确认</div>
          <div class="paid-desc">店主确认到账后预约将正式生效</div>
        </div>
      </template>
    </div>
    <!-- ─────────────────────────────────────────────── -->

    <!-- 店铺地址和指引模块 -->
    <div class="store-info-card">
      <div class="info-header">
        <van-icon name="location" size="20" color="var(--ink-900)" />
        <span class="info-text">{{ $t('home.location') }}</span>
      </div>
      <div class="guide-action">
        <van-button plain round block size="normal" color="var(--ink-500)" @click="showGuidePreview" class="apple-btn-active">
          {{ $t('home.showGuide') }}
        </van-button>
      </div>
    </div>

    <div class="bottom-action glass-effect">
      <van-button type="primary" block round size="large" @click="goHome" class="apple-btn-active">
        {{ $t('success.homeBtn') }}
      </van-button>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showImagePreview, showToast } from 'vant';
import { claimPaid, getDefaultDeposit } from '../api';

import wechatQr from '../assets/wechat-qr.png';
import alipayQr from '../assets/alipay-qr.png';

const router = useRouter();

// ─── 定金支付状态 ───────────────────────────────────
const depositAmount = ref(500);
const claiming = ref(false);
const depositStatus = ref('NONE');

onMounted(async () => {
  try {
    const res = await getDefaultDeposit();
    if (res.code === 200 && res.data) {
      depositAmount.value = res.data;
    }
  } catch (e) {
    // 默认 500，不影响页面
  }
});

const QR_MAP = {
  wechat: wechatQr,
  alipay: alipayQr,
};

const handlePay = (method) => {
  showImagePreview({ images: [QR_MAP[method]], closeable: true });
};

const handleClaimPaid = async () => {
  claiming.value = true;
  try {
    const reservationId = sessionStorage.getItem('lastReservationId');
    if (reservationId) {
      await claimPaid(reservationId);
    }
    depositStatus.value = 'CUSTOMER_PAID';
  } catch (e) {
    showToast('提交失败，请重试');
  } finally {
    claiming.value = false;
  }
};

// ─── 原有功能 ───────────────────────────────────────
const showGuidePreview = () => {
  router.push('/guide');
};

const goHome = () => {
  sessionStorage.removeItem('reserveDate');
  sessionStorage.removeItem('timeSlot');
  sessionStorage.removeItem('lastReservationId');
  router.replace('/');
};
</script>

<style scoped>
.success-result {
  text-align: center;
  padding: 48px 24px 120px 24px;
  background: transparent;
  min-height: 100vh;
  box-sizing: border-box;
  font-family: var(--font-cjk);
}
.result-icon {
  margin-bottom: 24px;
  animation: scale-up 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.title {
  font-size: 28px;
  color: var(--ink-900);
  font-weight: 700;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}
.desc {
  font-size: 16px;
  color: var(--ink-500);
  line-height: 1.6;
  margin-bottom: 32px;
}

/* ─── 定金支付卡片 (AWAI frosted card) ───────────── */
.payment-card {
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2);
  padding: 24px;
  margin-bottom: 24px;
  text-align: left;
  animation: fade-up 0.4s ease-out;
}
.payment-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 8px;
}
.payment-label {
  font-size: 16px;
  font-weight: 700;
  color: var(--pink-600);
}
.payment-amount {
  font-size: 24px;
  font-weight: 800;
  color: var(--pink-500);
}
.payment-desc {
  font-size: 13px;
  color: var(--ink-400);
  margin-bottom: 20px;
  line-height: 1.5;
}

/* ─── 支付方式按钮 (AWAI-style chips) ────────────── */
.payment-methods {
  display: flex;
  gap: 12px;
}
.method-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  background: var(--surface-card);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--dur-base) var(--ease-soft);
}
.method-item:active {
  transform: scale(0.96);
  background: var(--pink-50);
  border-color: var(--pink-300);
}
.method-item.paying {
  opacity: 0.6;
  pointer-events: none;
}
.method-icon {
  font-size: 28px;
}
.method-name {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-700);
}

/* ─── 已付款待确认状态 ─────────────────────────── */
.customer-paid-section {
  text-align: center;
  padding: 8px 0;
}
.paid-animation {
  font-size: 48px;
  margin-bottom: 8px;
}
.paid-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--ink-900);
}
.paid-desc {
  font-size: 14px;
  color: var(--ink-500);
  margin-top: 6px;
  line-height: 1.5;
}
/* ─────────────────────────────────────────────── */

.store-info-card {
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2);
  padding: 24px;
  margin-bottom: 40px;
}
.info-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}
.info-text {
  font-size: 16px;
  color: var(--ink-900);
  font-weight: 600;
  margin-left: 8px;
}

.bottom-action {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 24px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-top: 1px solid var(--border-soft);
}
@media (min-width: 768px) {
  .bottom-action {
    width: 390px;
    left: auto;
    right: 0;
  }
}

/* ─── 动画 ────────────────────────────────────────── */
@keyframes scale-up {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
@keyframes fade-up {
  0% { opacity: 0; transform: translateY(20px); }
  100% { opacity: 1; transform: translateY(0); }
}
</style>
