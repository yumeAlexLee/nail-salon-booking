<template>
  <div class="portfolio-page">
    <van-nav-bar :title="$t('home.portfolio')" left-arrow @click-left="goBack" class="glass-effect" fixed placeholder :border="false" />
    
    <div class="portfolio-desc">
      <h2>Nail Art Collection</h2>
      <p>精选美甲作品展示 / ネイルギャラリー</p>
    </div>

    <!-- ═══ 瀑布流双列排版 — AWAI frosted glass ═══ -->
    <div class="waterfall-container">
      <div class="waterfall-item" v-for="(img, index) in portfolioImages" :key="index" @click="previewImage(index)">
        <img :src="img" class="portfolio-img" alt="Nail Art Work" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { showImagePreview } from 'vant';

const router = useRouter();

// 预设作品集图片路径。未来只要把图片放在 public 目录下即可
const portfolioImages = ref([
  '/work1.jpg',
  '/work2.jpg',
  '/work3.jpg',
  '/work4.jpg',
  '/work5.jpg',
  '/work6.jpg',
  '/work7.jpg',
  '/work8.jpg',
  '/work9.jpg',
  '/work10.jpg',
  '/work11.jpg',
  '/work12.jpg',
  '/work13.jpg',
  '/work14.jpg',
  '/work15.jpg',
  '/work16.jpg',
  '/work17.jpg'
]);

const goBack = () => {
  router.back();
};

// 全屏沉浸式预览图片
const previewImage = (index) => {
  showImagePreview({
    images: portfolioImages.value,
    startPosition: index,
    closeable: true
  });
};
</script>

<style scoped>
.portfolio-page {
  min-height: 100vh;
  background: var(--surface-bg);
  padding-bottom: 60px;
}

/* ─── Description: AWAI frosted glass ─── */
.portfolio-desc {
  text-align: center;
  margin: 20px 20px 24px;
  padding: 22px 18px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2);
}
.portfolio-desc h2 {
  font-family: var(--font-cjk);
  font-weight: 800;
  font-size: 20px;
  color: var(--ink-900);
  margin: 0 0 6px 0;
  line-height: 1.3;
}
.portfolio-desc p {
  font-family: var(--font-cjk);
  font-size: 12.5px;
  color: var(--pink-600);
  margin: 0;
  letter-spacing: 0.02em;
}

/* ─── Waterfall: CSS columns + AWAI frosted glass ─── */
.waterfall-container {
  columns: 2;
  column-gap: 16px;
  padding: 0 16px;
}
.waterfall-item {
  break-inside: avoid;
  margin-bottom: 16px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-2);
  transition: all var(--dur-base) var(--ease-soft);
  cursor: pointer;
}
.waterfall-item:active {
  transform: scale(0.96);
  opacity: 0.9;
}
.portfolio-img {
  width: 100%;
  display: block;
  background: var(--pink-100);
  min-height: 100px;
}
</style>
