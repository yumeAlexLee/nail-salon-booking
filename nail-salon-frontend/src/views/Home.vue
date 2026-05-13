<template>
  <div class="home">
    <!-- ═══ Header: AWAI Wordmark ═══ -->
    <div style="text-align:center; padding: 32px 20px 10px;">
      <div class="awai-eyebrow">彩り · awai · nail studio</div>
      <div style="margin-top: 8px;">
        <span class="awai-wordmark">AWAI NAIL</span>
      </div>
    </div>

    <!-- ═══ 未识别身份 — 登录表单 ═══ -->
    <div v-if="!identified" style="padding: 12px 20px 28px;">
      <div class="frost-card" style="padding: 20px;">
        <div style="font-family:var(--font-cjk);font-weight:800;font-size:18px;color:var(--ink-900);text-align:center;">
          欢迎来到 AWAI ✨
        </div>
        <div style="font-family:var(--font-cjk);font-size:12.5px;color:var(--ink-500);margin-top:6px;margin-bottom:14px;text-align:center;">
          留个名字 + 联系方式，我们好记住你的喜好。
        </div>
        
        <div style="position:relative;margin-bottom:12px;">
          <i data-lucide="user" style="position:absolute;left:14px;top:50%;transform:translateY(-50%);width:17px;height:17px;color:var(--pink-600);stroke-width:1.8;z-index:1;"></i>
          <input v-model="userName" placeholder="姓名 · 怎么称呼你" class="awai-field" style="padding-left:38px;" />
        </div>
        
        <div style="position:relative;margin-bottom:8px;">
          <i data-lucide="at-sign" style="position:absolute;left:14px;top:50%;transform:translateY(-50%);width:17px;height:17px;color:var(--pink-600);stroke-width:1.8;z-index:1;"></i>
          <input v-model="contactId" placeholder="微信 / 手机号" class="awai-field" style="padding-left:38px;" />
        </div>
        <div style="font-family:var(--font-body);font-size:11.5px;color:var(--ink-500);margin-bottom:14px;">
          微信 / 手机号
        </div>

        <van-button type="primary" block round size="large" :loading="checking" @click="identifyUser" class="apple-btn-active" style="height:50px;font-weight:700;font-size:15.5px;">
          继续
        </van-button>
      </div>

      <!-- 语言切换 & 切换账号 (在已识别区域底部) -->
      <div style="display:flex;justify-content:center;margin-top:14px;">
        <button @click="toggleLang" style="background:transparent;border:0;cursor:pointer;font-family:var(--font-body);font-size:11px;color:var(--pink-600);letter-spacing:0.06em;">
          {{ currentLang === 'zh' ? '日本語' : '中文' }}
        </button>
      </div>
    </div>

    <!-- ═══ 已识别身份 ═══ -->
    <div v-else style="padding: 8px 20px 28px;">
      <!-- 欢迎横幅 -->
      <div class="frost-card" style="padding: 16px;margin-bottom:14px;">
        <div class="awai-eyebrow" style="color:var(--pink-600);">
          {{ isNew ? '初次到店 · first visit' : '回头客 · welcome back' }}
        </div>
        <div style="font-family:var(--font-cjk);font-weight:800;font-size:20px;color:var(--ink-900);margin-top:4px;line-height:1.25;">
          {{ isNew ? `欢迎，${userName} ✨` : `${userName}，又见面啦 🌷` }}
        </div>
        <div style="font-family:var(--font-cjk);font-size:12.5px;color:var(--ink-500);margin-top:4px;">
          {{ isNew ? '新客 8 折 · 首次预约自动减免，含咨询' : '一切如旧 · 查看或预约' }}
        </div>
        <button @click="resetUser" style="margin-top:10px;background:transparent;border:0;cursor:pointer;font-family:var(--font-body);font-size:11px;color:var(--pink-600);">
          换一个账号 ›
        </button>
      </div>

      <!-- 4 Tile Grid (v2) -->
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
        <!-- 作品集 -->
        <div class="awai-tile" @click="showPortfolio">
          <div class="awai-tile-icon" style="background:var(--pink-50);border:1px solid var(--pink-100);">
            <van-icon name="flower-o" size="22" color="var(--pink-500)" />
          </div>
          <div class="awai-tile-label">作品集</div>
          <div class="awai-tile-sub">浏览参考</div>
        </div>

        <!-- 开始预约 (pink gradient, featured) -->
        <div class="awai-tile awai-tile-primary" @click="startBooking">
          <div class="awai-tile-accent-circle"></div>
          <div class="awai-tile-icon" style="background:rgba(255,255,255,0.22);">
            <van-icon name="calendar-o" size="22" color="#fff" />
          </div>
          <div class="awai-tile-label" style="color:#fff;">开始预约</div>
          <div class="awai-tile-sub" style="color:rgba(255,255,255,0.88);">挑款式 + 时间</div>
        </div>

        <!-- 我的预约 -->
        <div class="awai-tile" @click="goToMyBookings">
          <div class="awai-tile-icon" style="background:var(--pink-50);border:1px solid var(--pink-100);">
            <van-icon name="records" size="22" color="var(--pink-500)" />
          </div>
          <div class="awai-tile-label">我的预约</div>
          <div class="awai-tile-sub">查看与取消</div>
        </div>

        <!-- 来店指引 -->
        <div class="awai-tile" @click="showGuidePreview">
          <div class="awai-tile-icon" style="background:var(--pink-50);border:1px solid var(--pink-100);">
            <van-icon name="location-o" size="22" color="var(--pink-500)" />
          </div>
          <div class="awai-tile-label">来店指引</div>
          <div class="awai-tile-sub">千葉県船橋市</div>
        </div>
      </div>
      
      <!-- 语言切换 -->
      <div style="display:flex;justify-content:center;margin-top:16px;">
        <button @click="toggleLang" style="background:transparent;border:0;cursor:pointer;font-family:var(--font-body);font-size:11px;color:var(--pink-600);letter-spacing:0.06em;">
          {{ currentLang === 'zh' ? '日本語' : '中文' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { getCustomerStatus } from '../api';
import i18n from '../i18n';

const router = useRouter();

const currentLang = ref(i18n.global.locale);
const userName = ref('');
const contactId = ref('');
const checking = ref(false);
const identified = ref(false);
const isNew = ref(false);

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
    showToast('请把姓名填一下');
    return;
  }
  if (!contactId.value.trim()) {
    showToast('请把联系方式填一下');
    return;
  }
  checking.value = true;
  try {
    const res = await getCustomerStatus(contactId.value.trim());
    if (res.code === 200) {
      isNew.value = res.data.isNew;
      identified.value = true;
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

const startBooking = () => router.push('/menu');
const showGuidePreview = () => router.push('/guide');
const showPortfolio = () => router.push('/portfolio');
const goToMyBookings = () => router.push('/my-bookings');

const toggleLang = () => {
  const next = currentLang.value === 'zh' ? 'ja' : 'zh';
  currentLang.value = next;
  i18n.global.locale = next;
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
  padding: 0;
  max-width: 600px;
  margin: 0 auto;
}

/* ─── AWAI Tiles ─── */
.awai-tile {
  border-radius: var(--radius-lg);
  padding: 18px 14px;
  cursor: pointer;
  transition: all var(--dur-base) var(--ease-soft);
  box-shadow: var(--shadow-1);
  border: 1px solid var(--border-soft);
  background: rgba(255,255,255,0.9);
}
.awai-tile:active {
  transform: scale(0.97);
  opacity: 0.85;
}
.awai-tile-icon {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  background: rgba(255,255,255,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
}
.awai-tile-label {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 14px;
  color: var(--ink-900);
  margin-bottom: 2px;
}
.awai-tile-sub {
  font-family: var(--font-cjk);
  font-size: 11px;
  color: var(--ink-500);
}

/* ─── Primary tile: pink gradient + accent circle ─── */
.awai-tile-primary {
  background: linear-gradient(135deg, var(--pink-400) 0%, var(--pink-500) 55%, var(--pink-600) 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
}
.awai-tile-accent-circle {
  position: absolute;
  top: -18px;
  right: -18px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
  pointer-events: none;
}
</style>
