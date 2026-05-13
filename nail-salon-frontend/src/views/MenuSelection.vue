<template>
  <div class="menu-selection">
    <van-nav-bar :title="$t('nav.menu')" left-arrow @click-left="goBack" class="glass-effect" fixed placeholder :border="false" />

    <!-- 加载中 -->
    <van-loading v-if="loading" size="24px" vertical style="margin-top: 80px;">{{ $t('date.loading') }}</van-loading>

    <!-- 动态菜单列表 -->
    <div v-else class="menu-list">
      <div v-for="(items, categoryKey) in menuData" :key="categoryKey" class="menu-category">
        <div class="category-title">{{ categoryKey }}</div>
        <van-card
          v-for="item in items"
          :key="item.id"
          :title="item.name"
          :desc="item.description"
          :price="isNewCustomer ? Math.round(item.price * 0.8).toLocaleString() : item.price.toLocaleString()"
          :origin-price="isNewCustomer ? item.price.toLocaleString() : undefined"
          class="menu-card"
          @click="goToBooking"
        >
          <template #thumb>
            <div class="menu-thumb" @click.stop="previewCategoryImages(items)">
              <img v-if="item.imageUrl" :src="item.imageUrl" class="thumb-img" />
              <span v-else class="thumb-placeholder">💅</span>
            </div>
          </template>
          <template #tags>
            <van-tag v-if="item.nameJa" plain type="primary" size="small">{{ item.nameJa }}</van-tag>
            <van-tag v-if="isNewCustomer" type="danger" size="small" style="margin-left:4px;">新客8折</van-tag>
          </template>
          <template #num>
            <div class="num-area">
              <span v-if="item.duration" class="duration-text">{{ item.duration }}分</span>
              <van-button size="mini" round plain type="primary" @click.stop="goToBooking">预约</van-button>
            </div>
          </template>
        </van-card>
      </div>
      <div v-if="Object.keys(menuData).length === 0" class="empty-tip">暂无可用的菜单项目</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showImagePreview } from 'vant';
import { getActiveMenu } from '../api';

const router = useRouter();
const isNewCustomer = sessionStorage.getItem('customerType') === 'NEW';
const menuData = ref({});
const loading = ref(true);

const goBack = () => {
  router.back();
};

const goToBooking = () => {
  router.push('/book');
};

const previewCategoryImages = (items) => {
  const images = items.map(item => item.imageUrl).filter(url => url);
  if (images.length > 0) {
    showImagePreview({ images, closeable: true });
  }
};

onMounted(async () => {
  try {
    const res = await getActiveMenu();
    if (res.code === 200) menuData.value = res.data;
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
  background-color: var(--apple-bg);
  display: flex;
  flex-direction: column;
}
/* 菜单列表 */
.menu-list {
  padding: 16px;
  flex: 1;
}
.menu-category {
  margin-bottom: 20px;
}
.category-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--apple-text);
  margin-bottom: 10px;
  padding-left: 4px;
}
.menu-card {
  margin-bottom: 10px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0,0,0,0.04);
  cursor: pointer;
}
:deep(.van-card) {
  background-color: var(--apple-white);
}
:deep(.van-card__title) {
  font-weight: 600;
  font-size: 15px;
}
:deep(.van-card__desc) {
  font-size: 12px;
  color: var(--apple-text-secondary);
}
:deep(.van-card__price) {
  color: #ee0a24;
  font-weight: 700;
  font-size: 16px;
}
.num-area {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.duration-text {
  font-size: 12px;
  color: var(--apple-text-secondary);
}
.num-area .van-button--mini {
  font-size: 11px;
  padding: 0 10px;
}

/* ─── 项目示意图缩略图 ──────────────────────────── */
.menu-thumb {
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f7;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
}
.thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}
.thumb-img:active {
  transform: scale(0.95);
}
.thumb-placeholder {
  font-size: 30px;
  opacity: 0.4;
}
/* ─────────────────────────────────────────────── */

.empty-tip {
  text-align: center;
  color: var(--apple-text-secondary);
  padding: 40px 0;
  font-size: 15px;
}
</style>
