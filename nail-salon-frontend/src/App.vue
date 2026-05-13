<template>
  <div class="app-layout">
    <!-- 桌面端左侧视觉区 -->
    <div class="desktop-visual">
      <!-- 带有暗色毛玻璃遮罩 -->
      <div class="visual-overlay"></div>
      <div class="visual-content">
        <h1>Nail Salon</h1>
        <p>发现指尖的艺术 · 专属您的美甲时光</p>
      </div>
    </div>
    
    <!-- 右侧或移动端全屏的应用区 -->
    <div class="mobile-container">
      <router-view></router-view>
    </div>
  </div>
</template>

<style>
/* 默认移动端：.desktop-visual 隐藏，.mobile-container 占满 */
.app-layout {
  display: flex;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background-color: var(--apple-bg);
}
.desktop-visual {
  display: none;
}
.mobile-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  position: relative;
  /* 关键技巧：让内部的 fixed 元素相对于此容器定位，而不是整个电脑窗口 */
  transform: translateZ(0);
  /* ─── 统一背景图 ────────────────────────────────
     把 bg.jpg 放到 public/ 目录下即可启用
     注释掉下一行恢复白色背景
     ──────────────────────────────────────────────── */
  background: url('/bg.jpg') center/cover fixed no-repeat;
}

/* 桌面端 (>= 768px) */
@media (min-width: 768px) {
  .desktop-visual {
    display: flex;
    flex: 1;
    align-items: center;
    justify-content: center;
    /* 可以在这里替换成您自己真实的美甲品牌背景图 */
    background: url('https://images.unsplash.com/photo-1604654894610-df63bc536371?auto=format&fit=crop&w=1600&q=80') center/cover no-repeat;
    position: relative;
  }
  .visual-overlay {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(4px);
    z-index: 1;
  }
  .visual-content {
    text-align: center;
    z-index: 2;
    color: var(--apple-white);
  }
  .visual-content h1 {
    font-size: 56px;
    margin-bottom: 16px;
    font-weight: 600;
    letter-spacing: -1px;
    text-shadow: 0 4px 12px rgba(0,0,0,0.2);
  }
  .visual-content p {
    font-size: 20px;
    color: rgba(255, 255, 255, 0.9);
    text-shadow: 0 2px 8px rgba(0,0,0,0.2);
  }
  .mobile-container {
    width: 414px; /* 完美模拟 iPhone 宽度 */
    min-width: 414px;
    height: 100vh;
    background-color: var(--apple-bg);
    box-shadow: -12px 0 40px rgba(0, 0, 0, 0.15);
    position: relative;
    z-index: 10;
  }
  
  /* 在电脑端定制滚动条，让它更细更优雅 */
  .mobile-container::-webkit-scrollbar {
    width: 6px;
  }
  .mobile-container::-webkit-scrollbar-track {
    background: transparent;
  }
  .mobile-container::-webkit-scrollbar-thumb {
    background-color: rgba(0,0,0,0.2);
    border-radius: 10px;
  }
}
</style>
