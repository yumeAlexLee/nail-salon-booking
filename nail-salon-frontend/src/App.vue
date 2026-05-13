<template>
  <div class="app-layout">
    <!-- 桌面端左侧视觉区 -->
    <div class="desktop-visual">
      <div class="visual-overlay"></div>
      <div class="visual-content">
        <div class="awai-eyebrow">彩り · awai · nail studio</div>
        <div class="awai-wordmark" style="margin-top:12px;font-size:72px;">AWAI NAIL</div>
        <p style="margin-top:20px;font-family:var(--font-cjk);color:rgba(255,255,255,0.85);font-size:18px;">
          发现指尖的艺术 · 专属妳的美甲时光
        </p>
      </div>
    </div>
    
    <!-- 右侧应用区 -->
    <div class="mobile-container" id="mobile-container">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup>
import { watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const bgMap = {
  '/': 'bg-home.jpg',
  '/menu': 'bg-menu.jpg',
  '/book': 'bg-menu.jpg',
  '/form': 'bg-menu.jpg',
  '/success': 'bg-home.jpg',
  '/guide': 'bg-home.jpg',
  '/portfolio': 'bg-portfolio.jpg',
  '/my-bookings': 'bg-home.jpg',
  '/admin': 'bg-kitty.jpg',
};

function updateBg(path) {
  const el = document.getElementById('mobile-container');
  if (!el) return;
  const bg = bgMap[path] || 'bg-home.jpg';
  el.style.backgroundImage = `url(/${bg})`;
}

onMounted(() => updateBg(route.path));
watch(() => route.path, updateBg);
</script>

<style>
/* ─── 基础布局 ─── */
.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}

/* ─── 桌面端左侧视觉区 ─── */
.desktop-visual {
  display: none;
}

/* ─── 移动端容器 ─── */
.mobile-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  background-size: cover;
  background-position: center;
  background-color: var(--pink-50);
  transition: background-image 0.4s var(--ease-soft);
}
/* 粉色半透明遮罩，让文字在背景图上清晰可见 */
.mobile-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(253, 243, 245, 0.25);
  pointer-events: none;
  z-index: 0;
}
/* 页面内容在遮罩之上 */
.mobile-container > :deep(*) {
  position: relative;
  z-index: 1;
}

/* ─── 桌面端 (>= 768px) ─── */
@media (min-width: 768px) {
  .desktop-visual {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    background: url('/bg-home.jpg') center/cover no-repeat;
    position: relative;
  }
  .visual-overlay {
    position: absolute;
    inset: 0;
    background: rgba(232, 127, 160, 0.20);
    backdrop-filter: blur(6px);
    z-index: 1;
  }
  .visual-content {
    text-align: center;
    z-index: 2;
    padding: 40px;
  }
  .mobile-container {
    width: 390px;
    min-width: 390px;
    height: 100vh;
    box-shadow: -8px 0 30px rgba(232, 127, 160, 0.14);
    position: relative;
    z-index: 10;
  }
  .mobile-container::-webkit-scrollbar {
    width: 6px;
  }
  .mobile-container::-webkit-scrollbar-track {
    background: transparent;
  }
  .mobile-container::-webkit-scrollbar-thumb {
    background-color: rgba(232, 127, 160, 0.3);
    border-radius: 10px;
  }
}
</style>
