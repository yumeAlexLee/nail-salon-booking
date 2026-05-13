<template>
  <div class="menu-selection">
    <!-- ═══ Nav ═══ -->
    <div class="menu-nav">
      <button class="menu-back" @click="goBack">
        <van-icon name="arrow-left" size="18" color="var(--ink-700)" />
      </button>
      <div class="menu-title">价目 · Menu</div>
      <div style="width:36px;"></div>
    </div>

    <van-loading v-if="loading" size="24px" vertical style="margin-top:80px;">加载中...</van-loading>

    <template v-else>
      <!-- ═══ 新客优惠卡片 ═══ -->
      <div v-if="isNewCustomer" class="discount-card">
        <!-- 星标装饰 -->
        <span class="discount-star">✦</span>
        <div class="discount-body">
          <div class="discount-title">新客 8 折优惠</div>
          <div class="discount-desc">首次预约自动减免，含咨询</div>
        </div>
        <div class="discount-tag">-20%</div>
      </div>

      <!-- ═══ 分类 Tabs ═══ -->
      <div class="menu-tabs">
        <button
          v-for="cat in categoryList"
          :key="cat"
          :class="['menu-tab', { active: activeTab === cat }]"
          @click="activeTab = cat"
        >{{ cat }}</button>
      </div>

      <!-- ═══ 服务列表 ═══ -->
      <div class="menu-items">
        <div
          v-for="item in currentItems"
          :key="item.id"
          class="menu-item-card"
          @click="goToBooking"
        >
          <!-- 缩略图 -->
          <div class="item-thumb">
            <img :src="item.imageUrl || '/' + thumbFallback(item)" class="item-img" />
          </div>

          <!-- 信息区 -->
          <div class="item-info">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-tag-row">
              <span v-if="item.nameJa" class="item-tag">{{ item.nameJa }}</span>
              <span v-if="isNewCustomer" class="item-tag item-tag-sale">新客 8 折</span>
            </div>
            <div class="item-duration">约 {{ item.duration }} 分钟</div>
          </div>

          <!-- 价格区 -->
          <div class="item-price-col">
            <div v-if="isNewCustomer" class="item-original">¥{{ item.price.toLocaleString() }}</div>
            <div class="item-price">
              ¥{{ isNewCustomer ? Math.round(item.price * 0.8).toLocaleString() : item.price.toLocaleString() }}
            </div>
          </div>
        </div>
      </div>

      <!-- ═══ 服务说明 ═══ -->
      <div class="menu-footer">
        <div class="footer-item">📐 款式甲来图报价 · 复杂度 ¥200–4,000</div>
        <div class="footer-item">🛡️ 10 天内含售后 · 放心体验</div>
        <div class="footer-item">🧼 一客一消毒 · 安心卫生</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getActiveMenu } from '../api';

const router = useRouter();
const isNewCustomer = sessionStorage.getItem('customerType') === 'NEW';
const menuData = ref({});
const loading = ref(true);
const activeTab = ref('');

const goBack = () => router.back();
const goToBooking = () => router.push('/book');

// 从 API 返回的 key 中提取中文分类名（格式："本甲|自爪ジェル"）
const categoryList = computed(() => {
  return Object.keys(menuData.value).map(k => k.split('|')[0]);
});

// 当前 Tab 选中的项目
const currentItems = computed(() => {
  if (!activeTab.value) return [];
  // 找匹配的 key
  const key = Object.keys(menuData.value).find(k => k.startsWith(activeTab.value));
  return key ? menuData.value[key] : [];
});

// 缩略图 fallback
const thumbFallback = (item) => {
  const map = {
    '换款卸甲·本甲': 'work4.jpg', '他店来·本甲': 'work8.jpg', '他店来·甲片': 'work11.jpg',
    '单色': 'work8.jpg', '裸色': 'work4.jpg', '简约·猫眼': 'work1.jpg', '轻奢·法式': 'work6.jpg',
    '高位半贴': 'work11.jpg', '塑形半贴': 'work15.jpg', '加长浅贴': 'work12.jpg',
    '叠加层次款式': 'work9.jpg', '人物手绘': 'work17.jpg', '手工造型': 'work5.jpg',
  };
  return map[item.name] || 'work1.jpg';
};

onMounted(async () => {
  try {
    const res = await getActiveMenu();
    if (res.code === 200) {
      menuData.value = res.data;
      const cats = Object.keys(res.data).map(k => k.split('|')[0]);
      if (cats.length > 0) activeTab.value = cats[0];
    }
  } catch (e) {
    console.error('加载菜单失败', e);
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.menu-selection {
  min-height: 100vh;
  background: transparent;
  padding-bottom: 30px;
}

/* ─── Nav ─── */
.menu-nav {
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
.menu-back {
  width: 36px; height: 36px;
  border-radius: 999px;
  background: rgba(255,255,255,0.85);
  border: 1px solid var(--pink-200);
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: var(--shadow-1);
  flex-shrink: 0;
}
.menu-back:active { transform: scale(0.95); }
.menu-title {
  flex: 1; text-align: center;
  font-family: var(--font-cjk);
  font-weight: 800; color: var(--ink-900);
  font-size: 15.5px; letter-spacing: 0.02em;
}

/* ─── Discount Card ─── */
.discount-card {
  margin: 14px 14px 0;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, #ff6b9d 0%, #e8436e 100%);
  color: #fff;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 4px 16px rgba(232, 67, 110, 0.35);
  position: relative;
  overflow: hidden;
}
.discount-star {
  font-size: 20px;
  color: #fcd34d;
  flex-shrink: 0;
  text-shadow: 0 0 6px rgba(252, 211, 77, 0.5);
}
.discount-body {
  flex: 1;
}
.discount-title {
  font-family: var(--font-cjk);
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  line-height: 1.3;
}
.discount-desc {
  font-family: var(--font-cjk);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.78);
  margin-top: 2px;
}
.discount-tag {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 18px;
  padding: 4px 10px;
  border-radius: 8px;
  background: #fef3c7;
  color: #d97706;
  flex-shrink: 0;
  line-height: 1;
}

/* ─── Tabs ─── */
.menu-tabs {
  display: flex;
  gap: 8px;
  padding: 12px 14px 8px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.menu-tabs::-webkit-scrollbar { display: none; }
.menu-tab {
  flex-shrink: 0;
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 13.5px;
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid var(--pink-200);
  background: var(--surface-frosted-strong);
  color: var(--ink-700);
  cursor: pointer;
  transition: all 220ms cubic-bezier(0.22,0.61,0.36,1);
}
.menu-tab.active {
  background: var(--pink-500);
  color: #fff;
  border-color: var(--pink-500);
  box-shadow: var(--shadow-2);
}

/* ─── Item Cards ─── */
.menu-items {
  padding: 0 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.menu-item-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: var(--radius-lg);
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-1);
  cursor: pointer;
  transition: all var(--dur-base) var(--ease-soft);
}
.menu-item-card:active { transform: scale(0.98); }

.item-thumb {
  width: 72px; height: 72px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--pink-100);
}
.item-img {
  width: 100%; height: 100%;
  object-fit: cover;
}

.item-info {
  flex: 1;
  min-width: 0;
}
.item-name {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 14px;
  color: var(--ink-900);
  line-height: 1.3;
}
.item-tag-row {
  display: flex;
  gap: 4px;
  margin-top: 4px;
  flex-wrap: wrap;
}
.item-tag {
  font-family: var(--font-cjk);
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--pink-50);
  color: var(--pink-600);
}
.item-tag-sale {
  background: var(--accent-red);
  color: #fff;
}
.item-duration {
  font-family: var(--font-cjk);
  font-size: 11px;
  color: var(--ink-500);
  margin-top: 4px;
}

.item-price-col {
  text-align: right;
  flex-shrink: 0;
}
.item-original {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--ink-400);
  text-decoration: line-through;
}
.item-price {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 17px;
  color: var(--pink-600);
  margin-top: 2px;
}

/* ─── Footer Notes ─── */
.menu-footer {
  margin: 20px 14px 0;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  background: var(--pink-50);
  border: 1px solid var(--border-soft);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.footer-item {
  font-family: var(--font-cjk);
  font-size: 12px;
  color: var(--ink-700);
  line-height: 1.4;
}
</style>
