<template>
  <div class="guide-page">
    <!-- ═══ Nav ═══ -->
    <div class="gp-nav">
      <button class="gp-back" @click="goBack">
        <van-icon name="arrow-left" size="18" color="var(--ink-700)" />
      </button>
      <div class="gp-title">来店指引</div>
      <div style="width:36px;"></div>
    </div>

    <!-- ═══ 地址卡片 ═══ -->
    <div class="addr-card">
      <div class="addr-city">📍 千葉県 · 船橋市</div>
      <div class="addr-detail">[ 详细地址私信告知 ]</div>

      <div class="addr-info">
        <div class="info-item">
          <van-icon name="location-o" size="14" color="var(--pink-500)" />
          <span>最近站 · 徒步 8 分</span>
        </div>
        <div class="info-item">
          <van-icon name="shop-o" size="14" color="var(--pink-500)" />
          <span>マルエツ (Maruetsu) 200 m</span>
        </div>
        <div class="info-item">
          <van-icon name="clock-o" size="14" color="var(--pink-500)" />
          <span>11:00 – 20:00 · 月曜定休</span>
        </div>
      </div>

      <button class="map-btn" @click="openMap">
        <van-icon name="map-marked" size="18" style="margin-right:6px;" />
        打开地图导航
      </button>
      <div class="map-hint">Google Maps / Apple Maps 跳转</div>
    </div>

    <!-- ═══ 4 步到店 ═══ -->
    <div class="steps-section">
      <div class="steps-title">📸 4 步到店</div>
      <div class="step-card" v-for="(img, i) in guideImages" :key="i">
        <div class="step-num">{{ i + 1 }}</div>
        <img :src="img" class="step-img" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';

const router = useRouter();
const goBack = () => router.back();

const guideImages = ref([
  '/guide1.jpg',
  '/guide2.jpg',
  '/guide3.jpg',
  '/guide4.jpg',
]);

const openMap = () => {
  const encoded = encodeURIComponent('千葉県船橋市飯山満');
  const url = `https://www.google.com/maps/search/?api=1&query=${encoded}`;
  window.open(url, '_blank');
  showToast('正在打开地图…');
};
</script>

<style scoped>
.guide-page {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 40px;
}

/* ─── Nav ─── */
.gp-nav {
  position: sticky; top: 0; z-index: 20;
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  background: rgba(253,243,245,0.78);
  border-bottom: 1px solid rgba(232,127,160,0.16);
  padding: 12px 14px;
  display: flex; align-items: center; gap: 10px;
}
.gp-back {
  width: 36px; height: 36px; border-radius: 999px;
  background: rgba(255,255,255,0.85);
  border: 1px solid var(--pink-200);
  display: grid; place-items: center; cursor: pointer;
  box-shadow: var(--shadow-1); flex-shrink: 0;
}
.gp-back:active { transform: scale(0.95); }
.gp-title {
  flex: 1; text-align: center;
  font-family: var(--font-cjk); font-weight: 800;
  color: var(--ink-900); font-size: 15.5px;
}

/* ─── Address card ─── */
.addr-card {
  margin: 14px 14px 0;
  padding: 20px 18px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
}
.addr-city {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 15px;
  color: var(--ink-900);
  margin-bottom: 4px;
}
.addr-detail {
  font-family: var(--font-cjk);
  font-size: 13px;
  color: var(--pink-600);
  font-weight: 500;
  margin-bottom: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-soft);
}
.addr-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-cjk);
  font-size: 13px;
  color: var(--ink-700);
}

/* Map button */
.map-btn {
  width: 100%;
  padding: 13px;
  border-radius: 999px;
  border: 0;
  background: var(--pink-500);
  color: #fff;
  font-family: var(--font-cjk);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-2);
  transition: all var(--dur-base) var(--ease-soft);
}
.map-btn:active { transform: scale(0.97); }
.map-hint {
  text-align: center;
  font-family: var(--font-cjk);
  font-size: 11px;
  color: var(--ink-400);
  margin-top: 6px;
}

/* ─── Steps ─── */
.steps-section {
  margin: 16px 14px 0;
}
.steps-title {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 16px;
  color: var(--ink-900);
  margin-bottom: 12px;
}
.step-card {
  position: relative;
  margin-bottom: 14px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-2);
  border: 1px solid var(--border-soft);
  background: var(--surface-frosted-strong);
}
.step-num {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--pink-500);
  color: #fff;
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-1);
}
.step-img {
  width: 100%;
  display: block;
}
</style>
