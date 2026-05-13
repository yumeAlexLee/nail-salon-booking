<template>
  <div class="portfolio-page">
    <!-- ═══ Nav: 圆返回 + 标题 ═══ -->
    <div class="pp-nav">
      <button class="pp-back" @click="goBack">
        <van-icon name="arrow-left" size="18" color="var(--ink-700)" />
      </button>
      <div class="pp-title">作品集 · {{ filteredList.length }} 款</div>
      <div style="width:36px;"></div>
    </div>

    <!-- ═══ 标签分类栏 ═══ -->
    <div class="pp-tags-wrapper">
      <div class="pp-tags">
        <button
          v-for="tag in allTags"
          :key="tag"
          :class="['pp-tag', { active: currentTag === tag }]"
          @click="currentTag = tag"
        >{{ tag }}</button>
      </div>
    </div>

    <!-- ═══ 双列宫格 ═══ -->
    <div class="pp-grid">
      <div
        v-for="(item, index) in filteredList"
        :key="item.f"
        class="pp-card"
        @click="previewImage(index)"
      >
        <img :src="'/' + item.f" class="pp-img" alt="nail" />
        <span class="pp-badge">{{ item.tag }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { showImagePreview } from 'vant';

const router = useRouter();
const goBack = () => router.back();

// 作品集数据（含标签）
const portfolio = [
  { f: 'work1.jpg',  tag: '猫眼' }, { f: 'work2.jpg',  tag: '猫眼' },
  { f: 'work3.jpg',  tag: '蹭粉' }, { f: 'work4.jpg',  tag: '裸色' },
  { f: 'work5.jpg',  tag: '手绘' }, { f: 'work6.jpg',  tag: '法式' },
  { f: 'work7.jpg',  tag: '猫眼' }, { f: 'work8.jpg',  tag: '裸色' },
  { f: 'work9.jpg',  tag: '渐变' }, { f: 'work10.jpg', tag: '手绘' },
  { f: 'work11.jpg', tag: '碎钻' },{ f: 'work12.jpg', tag: '轻奢' },
  { f: 'work13.jpg', tag: '渐变' },{ f: 'work14.jpg', tag: '猫眼' },
  { f: 'work15.jpg', tag: '碎钻' },{ f: 'work16.jpg', tag: '猫眼' },
  { f: 'work17.jpg', tag: '手绘' },
];

const allTags = ['全部', '猫眼', '蹭粉', '裸色', '手绘', '法式', '渐变', '碎钻', '轻奢'];
const currentTag = ref('全部');

const filteredList = computed(() =>
  currentTag.value === '全部'
    ? portfolio
    : portfolio.filter(p => p.tag === currentTag.value)
);

const previewImage = (index) => {
  const imgs = filteredList.value.map(p => '/' + p.f);
  showImagePreview({ images: imgs, startPosition: index, closeable: true });
};
</script>

<style scoped>
.portfolio-page {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 40px;
}

/* ─── Nav ─── */
.pp-nav {
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  background: rgba(253, 243, 245, 0.78);
  border-bottom: 1px solid rgba(232, 127, 160, 0.16);
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.pp-back {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid var(--pink-200);
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: var(--shadow-1);
  flex-shrink: 0;
}
.pp-back:active {
  transform: scale(0.95);
}
.pp-title {
  flex: 1;
  text-align: center;
  font-family: var(--font-cjk);
  font-weight: 800;
  color: var(--ink-900);
  font-size: 15.5px;
  letter-spacing: 0.02em;
}

/* ─── Tags ─── */
.pp-tags-wrapper {
  padding: 12px 14px 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.pp-tags-wrapper::-webkit-scrollbar {
  display: none;
}
.pp-tags {
  display: flex;
  gap: 8px;
  white-space: nowrap;
}
.pp-tag {
  font-family: var(--font-cjk);
  font-weight: 600;
  font-size: 12.5px;
  padding: 7px 14px;
  border-radius: 999px;
  border: 1px solid var(--pink-200);
  background: var(--pink-50);
  color: var(--pink-700);
  cursor: pointer;
  transition: all 220ms cubic-bezier(0.22, 0.61, 0.36, 1);
}
.pp-tag.active {
  background: var(--pink-500);
  color: #fff;
  border-color: var(--pink-500);
  box-shadow: var(--shadow-2);
}

/* ─── Grid ─── */
.pp-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 4px 14px 20px;
}
.pp-card {
  position: relative;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-2);
  border: 1px solid var(--border-soft);
  cursor: pointer;
  transition: all var(--dur-base) var(--ease-soft);
  background: var(--surface-frosted-strong);
}
.pp-card:active {
  transform: scale(0.97);
}
.pp-img {
  width: 100%;
  display: block;
  background: var(--pink-100);
  min-height: 160px;
}
.pp-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  font-family: var(--font-cjk);
  font-weight: 600;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--ink-700);
  pointer-events: none;
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
}
</style>
