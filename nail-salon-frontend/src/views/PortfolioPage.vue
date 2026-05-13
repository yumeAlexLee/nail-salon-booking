<template>
  <div class="portfolio-page">
    <van-nav-bar :title="$t('home.portfolio')" left-arrow @click-left="goBack" class="glass-effect" fixed placeholder :border="false" />
    
    <div class="portfolio-desc">
      <h2>Nail Art Collection</h2>
      <p>精选美甲作品展示 / ネイルギャラリー</p>
    </div>

    <!-- 瀑布流双列排版 -->
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
  background-color: var(--apple-bg);
  padding-bottom: 60px;
}
.portfolio-desc {
  text-align: center;
  padding: 30px 20px 20px;
}
.portfolio-desc h2 {
  font-family: 'Noto Serif SC', serif;
  font-size: 24px;
  color: var(--apple-text);
  margin: 0 0 8px 0;
}
.portfolio-desc p {
  font-size: 13px;
  color: var(--apple-text-secondary);
  margin: 0;
}
.waterfall-container {
  /* 使用纯 CSS columns 实现瀑布流效果 */
  columns: 2;
  column-gap: 16px;
  padding: 0 16px;
}
.waterfall-item {
  break-inside: avoid;
  margin-bottom: 16px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(255, 107, 139, 0.1);
  background-color: var(--apple-white);
  transition: transform 0.2s cubic-bezier(0.2, 0, 0, 1);
  cursor: pointer;
}
.waterfall-item:active {
  transform: scale(0.96);
  opacity: 0.9;
}
.portfolio-img {
  width: 100%;
  display: block;
  /* 加入一个浅色底，防止图片没加载出来时难看 */
  background-color: #fce4ec;
  min-height: 100px;
}
</style>
