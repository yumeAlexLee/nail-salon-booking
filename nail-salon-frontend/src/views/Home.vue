<template>
  <div class="home">
    <!-- ═══ Header ═══ -->
    <div class="header">
      <div class="header-bg"></div>
      <div class="header-content">
        <div class="lang-switch">
          <van-button plain round size="small" type="default" @click="toggleLang" class="lang-btn">{{ $t('home.langToggle') }}</van-button>
        </div>
        <h1 class="title">{{ $t('nav.book') }}</h1>
      </div>
    </div>

    <!-- ═══ 状态 1：未识别身份 — 登录表单 ═══ -->
    <div v-if="!identified" class="login-section">
      <div class="login-card">
        <div class="login-title">{{ $t('home.loginName') }}</div>
        <van-field
          v-model="userName"
          :placeholder="$t('home.loginNamePlaceholder')"
          clearable
          size="large"
          :border="false"
        >
          <template #left-icon>
            <van-icon name="contact" size="20" color="var(--apple-blue)" />
          </template>
        </van-field>
        <div class="login-title" style="margin-top: 4px;">{{ $t('home.loginContact') }}</div>
        <van-field
          v-model="contactId"
          :placeholder="$t('home.loginContactPlaceholder')"
          clearable
          size="large"
          :border="false"
        >
          <template #left-icon>
            <van-icon name="phone-o" size="20" color="var(--apple-blue)" />
          </template>
        </van-field>
        <div style="margin-top: 20px;">
          <van-button type="primary" block round size="large" :loading="checking" @click="identifyUser" class="apple-btn-active">
            {{ $t('home.loginBtn') }}
          </van-button>
        </div>
      </div>
    </div>

    <!-- ═══ 状态 2：已识别身份 ═══ -->
    <div v-else class="dashboard-section">
      <!-- 身份横幅 -->
      <div class="identity-banner" :class="{ 'is-new': isNew, 'is-old': !isNew }">
        <div class="banner-icon">{{ isNew ? '🌟' : '🔄' }}</div>
        <div class="banner-text">
          <div class="banner-greeting">{{ isNew ? $t('home.welcomeNew') : $t('home.welcomeOld') }}</div>
          <div class="banner-name">{{ userName }}</div>
          <div class="banner-type">{{ isNew ? $t('home.newCustomer') : $t('home.oldCustomer') }}</div>
        </div>
      </div>

      <!-- ═══ 左侧大卡片 + 右侧我的预约 ═══ -->
      <div class="main-row">
        <div class="big-card">
          <div class="big-card-row top-row" @click="showPortfolio">
            <span class="big-card-icon">🖼️</span>
            <div class="big-card-text">
              <div class="big-card-label">{{ $t('home.portfolio') }}</div>
              <div class="big-card-desc">查看作品</div>
            </div>
            <van-icon name="arrow" class="big-card-arrow" />
          </div>
          <div class="big-card-row bottom-row" @click="startBooking">
            <span class="big-card-icon">{{ isNew ? '🌟' : '💅' }}</span>
            <div class="big-card-text">
              <div class="big-card-label">{{ isNew ? $t('home.firstVisit') : $t('home.startBooking') }}</div>
              <div v-if="isNew" class="big-card-desc promo">首次 8 折优惠</div>
              <div v-else class="big-card-desc">选择美甲菜单</div>
            </div>
            <van-icon name="arrow" class="big-card-arrow" />
          </div>
        </div>

        <div class="bookings-card" @click="goToMyBookings">
          <span class="bc-icon">📋</span>
          <span class="bc-label">{{ $t('booking.myBookings') }}</span>
        </div>
      </div>
    </div>

    <!-- ═══ 地址信息 ═══ -->
    <div class="address-bar" @click="showGuidePreview">
      <svg class="addr-map-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#ff6b8b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
        <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
        <circle cx="12" cy="10" r="3"/>
      </svg>
      <span class="addr-text">{{ $t('home.location') }}</span>
      <span class="addr-spacer"></span>
      <span class="switch-link" @click.stop="resetUser">切换</span>
      <van-icon name="arrow" class="addr-arrow" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { showToast } from 'vant';
import { getCustomerStatus } from '../api';

const router = useRouter();
const { locale } = useI18n();

// 登录状态
const userName = ref('');
const contactId = ref('');
const checking = ref(false);
const identified = ref(false);
const isNew = ref(false);

// ─── 恢复已识别状态 ─────────────────────────────
// 从 Home 跳转到其他页面再 router.back() 回来时，
// Home 组件会重新挂载，identified 丢失。
// 从 sessionStorage 恢复，避免用户重新输入信息。
onMounted(() => {
  const storedName = sessionStorage.getItem('userName');
  const storedContact = sessionStorage.getItem('userContactId');
  const storedType = sessionStorage.getItem('customerType');
  if (storedName && storedContact && storedType) {
    userName.value = storedName;
    contactId.value = storedContact;
    isNew.value = storedType === 'NEW';
    identified.value = true;
  }
});

const identifyUser = async () => {
  if (!userName.value.trim()) {
    showToast('请输入姓名');
    return;
  }
  if (!contactId.value.trim()) {
    showToast('请输入联系方式');
    return;
  }

  checking.value = true;
  try {
    const res = await getCustomerStatus(contactId.value.trim());
    if (res.code === 200) {
      isNew.value = res.data.isNew;
      identified.value = true;

      // 保存到 sessionStorage，供后续页面使用
      sessionStorage.setItem('customerType', res.data.customerType);
      sessionStorage.setItem('userName', userName.value.trim());
      sessionStorage.setItem('userContactId', contactId.value.trim());
    } else {
      showToast(res.message || '验证失败');
    }
  } catch (e) {
    showToast('网络异常，请重试');
  } finally {
    checking.value = false;
  }
};

const startBooking = () => {
  router.push('/menu');
};

const showGuidePreview = () => {
  router.push('/guide');
};

const showPortfolio = () => {
  router.push('/portfolio');
};

const goToMyBookings = () => {
  router.push('/my-bookings');
};

const toggleLang = () => {
  locale.value = locale.value === 'zh' ? 'ja' : 'zh';
};

const resetUser = () => {
  identified.value = false;
  userName.value = '';
  contactId.value = '';
  sessionStorage.removeItem('customerType');
  sessionStorage.removeItem('userName');
  sessionStorage.removeItem('userContactId');
};
</script>

<style scoped>
.home {
  padding: 24px 24px 40px 24px;
  max-width: 600px;
  margin: 0 auto;
}

/* ═══════════════════════════════════════════════
   ① Header — 增强粉色描边发光 + 圆润艺术字体
   ═══════════════════════════════════════════════ */
.header {
  text-align: center;
  margin-bottom: 24px;
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  padding: 60px 20px 50px 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.06);
}
.header-bg {
  position: absolute;
  top: -20px; left: -20px; right: -20px; bottom: -20px;
  background: url('/kitty-bg.jpg') center/cover no-repeat, linear-gradient(135deg, #fff0f3 0%, #ffc2d1 100%);
  z-index: 1;
}
.header-bg::after {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(2px);
  -webkit-backdrop-filter: blur(2px);
}
.header-content {
  position: relative;
  z-index: 2;
}
.lang-switch {
  position: absolute;
  top: -40px;
  right: 0;
}
.lang-btn {
  border-color: transparent !important;
  color: #ff477e !important;
  background-color: rgba(255, 255, 255, 0.9) !important;
  box-shadow: 0 2px 10px rgba(255, 107, 139, 0.2);
  font-weight: 600;
}
.title {
  font-family: 'ZCOOL KuaiLe', cursive;
  font-weight: 400;
  color: #ff477e;
  font-size: 46px;
  margin: 0 0 12px 0;
  letter-spacing: 3px;
  text-shadow:
    3px 3px 0 #fff, -3px -3px 0 #fff,
    3px -3px 0 #fff, -3px 3px 0 #fff,
    0 0 20px rgba(255, 107, 139, 0.6),
    0 0 40px rgba(255, 107, 139, 0.3),
    0 4px 12px rgba(255, 107, 139, 0.5);
}

/* ═══════════════════════════════════════════════
   ② 登录表单
   ═══════════════════════════════════════════════ */
.login-section {
  margin-bottom: 24px;
}
.login-card {
  background: var(--apple-white);
  border-radius: 20px;
  padding: 24px 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.04);
}
.login-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text-secondary);
  margin-bottom: 4px;
  padding: 0 12px;
}
.login-card .van-field {
  background: var(--apple-bg);
  border-radius: 14px;
  margin: 4px 0;
}

/* ═══════════════════════════════════════════════
   ③ 已识别 — 身份横幅
   ═══════════════════════════════════════════════ */
.identity-banner {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
}
.identity-banner.is-new {
  background: linear-gradient(135deg, #fff0f3 0%, #ffd6e0 100%);
  border: 1px solid #ffb3c6;
}
.identity-banner.is-old {
  background: linear-gradient(135deg, #e6fffb 0%, #b8f0e8 100%);
  border: 1px solid #87e8de;
}
.banner-icon {
  font-size: 36px;
  flex-shrink: 0;
}
.banner-text {
  flex: 1;
}
.banner-greeting {
  font-size: 15px;
  font-weight: 600;
  color: var(--apple-text);
}
.banner-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--apple-text);
  margin-top: 2px;
}
.banner-type {
  font-size: 13px;
  color: var(--apple-text-secondary);
  margin-top: 4px;
}

/* ═══════════════════════════════════════════════
   ④ 大卡片：作品集 + 菜单
   ═══════════════════════════════════════════════ */
.big-card {
  background: transparent;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.04);
  margin-bottom: 12px;
}
.big-card-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 20px 18px;
  cursor: pointer;
  transition: all 0.15s;
  min-height: 64px;
}
.big-card-row:active {
  opacity: 0.7;
}
.top-row {
  background: linear-gradient(135deg, rgba(255,245,247,0.60) 0%, rgba(255,228,232,0.55) 100%),
              url('/portfolio-bg.jpg') center/cover no-repeat;
  border-radius: 20px;
}
.bottom-row {
  background: linear-gradient(135deg, rgba(255,248,240,0.60) 0%, rgba(255,237,213,0.55) 100%),
              url('/menu-bg.jpg') center / 125% no-repeat;
  border-radius: 20px;
  margin-top: 8px;
}
.big-card-icon {
  font-size: 30px;
  flex-shrink: 0;
  width: 44px;
  text-align: center;
}
.big-card-text {
  flex: 1;
}
.big-card-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--apple-text);
}
.big-card-desc {
  font-size: 13px;
  color: var(--apple-text-secondary);
  margin-top: 2px;
}
.big-card-desc.promo {
  color: #ee0a24;
  font-weight: 500;
}
.big-card-arrow {
  font-size: 14px;
  color: rgba(0,0,0,0.15);
  flex-shrink: 0;
}

/* ═══════════════════════════════════════════════
   ⑤ 主行：左侧大卡片 + 右侧预约卡片
   ═══════════════════════════════════════════════ */
.main-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.main-row .big-card {
  flex: 1;
  margin-bottom: 0;
}

.bookings-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 88px;
  flex-shrink: 0;
  background: linear-gradient(160deg, #e6f7ff, #bae7ff);
  border-radius: 20px;
  box-shadow: 0 6px 20px rgba(24, 144, 255, 0.08);
  cursor: pointer;
  transition: all 0.15s;
  padding: 12px 4px;
}
.bookings-card:active {
  transform: scale(0.95);
  opacity: 0.8;
}
.bc-icon {
  font-size: 28px;
  line-height: 1;
}
.bc-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--apple-text);
  text-align: center;
  line-height: 1.3;
}

/* ═══════════════════════════════════════════════
   ⑥ 地址信息 — SVG 地图图标 + 切换账号
   ═══════════════════════════════════════════════ */
.address-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  background: var(--apple-white);
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(255, 107, 139, 0.06);
  cursor: pointer;
}
.address-bar:active {
  opacity: 0.7;
}
.addr-map-icon {
  flex-shrink: 0;
}
.addr-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--apple-text);
}
.addr-spacer {
  flex: 1;
}
.switch-link {
  font-size: 12px;
  color: var(--apple-blue);
  font-weight: 500;
  padding: 2px 6px;
  cursor: pointer;
  opacity: 0.7;
}
.switch-link:active {
  opacity: 1;
}
.addr-arrow {
  font-size: 14px;
  color: rgba(0,0,0,0.15);
  margin-left: 4px;
}
</style>
