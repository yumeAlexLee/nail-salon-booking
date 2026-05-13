<template>
  <div class="guide-page">
    <van-nav-bar :title="$t('home.showGuide')" left-arrow @click-left="goBack" class="glass-effect" fixed placeholder :border="false" />
    
    <div class="images-container">
      <img v-for="(img, index) in guideImages" :key="index" :src="img" class="guide-image" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const router = useRouter();

// 在这里配置您的多张指引图名称
const guideImages = ref([
  '/guide1.jpg',
  '/guide2.jpg',
  '/guide3.jpg',
  '/guide4.jpg'
]);

const goBack = () => {
  router.back();
};
</script>

<style scoped>
.guide-page {
  min-height: 100vh;
  background-color: var(--apple-bg);
}
.images-container {
  display: flex;
  flex-direction: column;
  padding: 20px 16px 40px 16px;
  gap: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.guide-image {
  width: 100%;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  display: block;
}
</style>
