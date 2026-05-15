<template>
  <div class="booking-form">
    <!-- ═══ Nav ═══ -->
    <div class="bf-nav">
      <button class="bf-back" @click="goBack">
        <van-icon name="arrow-left" size="18" color="var(--ink-700)" />
      </button>
      <div class="bf-title">填写信息</div>
      <div style="width:36px;"></div>
    </div>

    <!-- ═══ 预约摘要 ═══ -->
    <div class="summary-card">
      <div class="summary-row" v-if="selectedService">
        <span class="summary-label">{{ selectedService.name }}</span>
        <span class="summary-price">¥{{ isNew ? Math.round(selectedService.price * 0.8).toLocaleString() : selectedService.price.toLocaleString() }}</span>
      </div>
      <div class="summary-sub-list" v-if="selectedOptions.length > 0">
        <div class="summary-sub-item" v-for="opt in selectedOptions" :key="opt.id">
          <span class="summary-sub-name">+ {{ opt.name }}</span>
          <span class="summary-sub-price">+¥{{ opt.price.toLocaleString() }}</span>
        </div>
      </div>
      <div class="summary-row">
        <span class="summary-label">{{ reserveDate }} {{ timeSlot }}</span>
        <span class="summary-meta" v-if="selectedService">约 {{ totalDuration }} 分</span>
      </div>
    </div>

    <!-- ═══ 定金说明 ═══ -->
    <div class="deposit-box">
      <div class="deposit-title">💎 预约需预付定金</div>
      <div class="deposit-amt">¥{{ depositAmount.toLocaleString() }}</div>
      <div class="deposit-desc">需提前 24 小时取消，否则定金不退。</div>
    </div>

    <!-- ═══ 表单 ═══ -->
    <div class="form-section">
      <!-- 姓名 -->
      <div class="field-group">
        <label class="field-label">姓名</label>
        <input v-model="form.name" placeholder="怎么称呼你" class="awai-field" />
      </div>

      <!-- 联系方式 -->
      <div class="field-group">
        <label class="field-label">联系方式</label>
        <input v-model="form.contactId" placeholder="小红书 / WeChat / LINE" class="awai-field" />
        <div class="field-hint">使用首页登录的信息</div>
      </div>

      <!-- 卸甲类型 -->
      <div class="field-group">
        <label class="field-label">卸甲类型</label>
        <div class="chip-row">
          <button
            v-for="opt in removalOptions"
            :key="opt.value"
            :class="['awai-chip', { selected: form.removalType === opt.value }]"
            @click="form.removalType = opt.value"
          >{{ opt.label }}<span v-if="opt.price > 0" class="chip-price"> +¥{{ opt.price.toLocaleString() }}</span></button>
        </div>
      </div>

      <!-- 备注 -->
      <div class="field-group">
        <label class="field-label">备注（选填）</label>
        <textarea
          v-model="form.remarks"
          class="awai-field"
          rows="3"
          placeholder="颜色偏好 / 长度 / 过敏情况等"
          maxlength="200"
        ></textarea>
      </div>

      <!-- 参考图上传 -->
      <div class="field-group">
        <label class="field-label">参考图（选填 · 最多 3 张）</label>
        <van-uploader v-model="fileList" :after-read="afterRead" :max-count="3" multiple />
      </div>
    </div>

    <!-- ═══ 底部 ═══ -->
    <div class="bf-bottom">
      <button class="bf-submit" :disabled="loading" @click="onSubmit">
        <van-loading v-if="loading" size="18" color="#fff" style="margin-right:6px;" />
        {{ loading ? '提交中...' : '确认预约' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { submitReservation, uploadImage } from '../api';
import { showDialog } from 'vant';

const router = useRouter();

const selectedService = JSON.parse(sessionStorage.getItem('selectedService') || 'null');
const isNew = sessionStorage.getItem('customerType') === 'NEW';
const selectedOptions = JSON.parse(sessionStorage.getItem('selectedOptions') || '[]');
const selectedOptionIds = JSON.parse(sessionStorage.getItem('selectedOptionIds') || '[]');
const reserveDate = ref('');
const timeSlot = ref('');
const loading = ref(false);

// 总时长（主服务 + 子选项）
const totalDuration = computed(() => {
  if (!selectedService) return 0;
  const base = selectedService.duration || 0;
  const extra = selectedOptions.reduce((sum, o) => sum + (o.duration || 0), 0);
  return Math.max(120, base + extra);
});

// 总金额
const totalPrice = computed(() => {
  let price = selectedService ? (isNew ? Math.round(selectedService.price * 0.8) : selectedService.price) : 0;
  price += selectedOptions.reduce((sum, o) => sum + (o.price || 0), 0);
  // 加上卸甲费用
  const removal = removalOptions.find(r => r.value === form.value.removalType);
  if (removal) price += removal.price;
  return price;
});

// 定金：30%
const depositAmount = computed(() => {
  if (!selectedService) return 500;
  return Math.ceil(totalPrice.value * 0.3);
});

const removalOptions = [
  { value: '本店续做', label: '本店续做（免费）', price: 0 },
  { value: '他店来·本甲', label: '他店来·本甲', price: 600 },
  { value: '他店来·甲片', label: '他店来·甲片', price: 1000 },
  { value: '无', label: '不需要卸甲', price: 0 },
];

const form = ref({
  name: sessionStorage.getItem('userName') || '',
  contactId: sessionStorage.getItem('userContactId') || '',
  removalType: '本店续做',
  remarks: '',
  referenceImage: '',
});

const fileList = ref([]);

const afterRead = async (file) => {
  file.status = 'uploading';
  file.message = '上传中...';
  try {
    const fd = new FormData();
    fd.append('file', file.file);
    const res = await uploadImage(fd);
    if (res.code === 200) {
      file.status = 'done';
      form.value.referenceImage = res.data;
    } else {
      file.status = 'failed';
      file.message = '上传失败';
    }
  } catch {
    file.status = 'failed';
    file.message = '上传失败';
  }
};

onMounted(() => {
  reserveDate.value = sessionStorage.getItem('reserveDate') || '';
  timeSlot.value = sessionStorage.getItem('timeSlot') || '';
  if (!reserveDate.value || !timeSlot.value) {
    router.replace('/');
  }
});

const goBack = () => router.back();

const onSubmit = async () => {
  if (!form.value.name.trim()) { showDialog({ message: '请填写姓名' }); return; }
  if (!form.value.contactId.trim()) { showDialog({ message: '请填写联系方式' }); return; }
  
  loading.value = true;
  try {
    const data = {
      ...form.value,
      customerType: isNew ? 'NEW' : 'OLD',
      reserveDate: reserveDate.value,
      timeSlot: timeSlot.value,
      menuItemId: selectedService?.id,
      optionIds: selectedOptionIds.length > 0 ? selectedOptionIds : null,
      totalAmount: totalPrice.value,
      totalDuration: totalDuration.value,
    };
    const res = await submitReservation(data);
    if (res.code === 200) {
      if (res.data) sessionStorage.setItem('lastReservationId', String(res.data));
      sessionStorage.setItem('depositAmount', String(depositAmount.value));
      router.replace('/success');
    } else {
      showDialog({ title: '提交失败', message: res.message });
    }
  } catch (error) {
    const msg = error?.response?.data?.message || '网络异常，请重试';
    showDialog({ title: '错误', message: msg });
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.booking-form {
  min-height: 100vh;
  background: transparent;
  display: flex;
  flex-direction: column;
}

/* ─── Nav ─── */
.bf-nav {
  flex-shrink: 0;
  position: sticky; top: 0; z-index: 20;
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  background: rgba(253,243,245,0.78);
  border-bottom: 1px solid rgba(232,127,160,0.16);
  padding: 12px 14px;
  display: flex; align-items: center; gap: 10px;
}
.bf-back {
  width: 36px; height: 36px; border-radius: 999px;
  background: rgba(255,255,255,0.85);
  border: 1px solid var(--pink-200);
  display: grid; place-items: center; cursor: pointer;
  box-shadow: var(--shadow-1); flex-shrink: 0;
}
.bf-back:active { transform: scale(0.95); }
.bf-title {
  flex: 1; text-align: center;
  font-family: var(--font-cjk); font-weight: 800;
  color: var(--ink-900); font-size: 15.5px;
}

/* ─── Summary ─── */
.summary-card {
  margin: 14px 14px 0;
  padding: 14px 16px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
}
.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
}
.summary-row + .summary-row {
  border-top: 1px solid var(--border-soft);
  margin-top: 6px;
  padding-top: 10px;
}
.summary-label {
  font-family: var(--font-cjk);
  font-weight: 600;
  font-size: 14px;
  color: var(--ink-900);
}
.summary-price {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 17px;
  color: var(--pink-600);
}
.summary-meta {
  font-family: var(--font-body);
  font-size: 12px;
  color: var(--ink-500);
}
.summary-sub-list {
  margin: 6px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.summary-sub-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-left: 10px;
}
.summary-sub-name {
  font-family: var(--font-cjk);
  font-size: 12px;
  color: var(--ink-600);
}
.summary-sub-price {
  font-family: var(--font-body);
  font-size: 12px;
  font-weight: 600;
  color: var(--pink-600);
}

/* ─── Deposit ─── */
.deposit-box {
  margin: 12px 14px 0;
  padding: 14px 16px;
  background: #fef9e7;
  border: 1px solid #fdecc8;
  border-radius: var(--radius-md);
}
.deposit-title {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 14px;
  color: #92400e;
}
.deposit-amt {
  font-family: var(--font-body);
  font-weight: 800;
  font-size: 20px;
  color: #d97706;
  margin-top: 4px;
}
.deposit-desc {
  font-family: var(--font-cjk);
  font-size: 12px;
  color: #a16207;
  margin-top: 4px;
  line-height: 1.4;
}

/* ─── Form ─── */
.form-section {
  margin: 14px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  flex: 1;
}
.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field-label {
  font-family: var(--font-cjk);
  font-weight: 700;
  font-size: 13px;
  color: var(--ink-700);
}
.field-hint {
  font-family: var(--font-cjk);
  font-size: 11px;
  color: var(--ink-400);
  margin-top: -2px;
}

/* ─── Chips ─── */
.chip-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip-price {
  font-weight: 600;
  color: var(--pink-600);
  margin-left: 2px;
}

/* ─── Bottom ─── */
.bf-bottom {
  margin-top: auto;
  padding: 12px 14px 24px;
}
.bf-submit {
  width: 100%;
  padding: 15px;
  border-radius: 999px;
  border: 0;
  background: linear-gradient(135deg, #ff6b9d 0%, #e8436e 100%);
  color: #fff;
  font-family: var(--font-cjk);
  font-size: 16px; font-weight: 700;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 4px 16px rgba(232,67,110,0.35);
  transition: all var(--dur-base) var(--ease-soft);
}
.bf-submit:active { transform: scale(0.97); }
.bf-submit:disabled { opacity: 0.6; cursor: not-allowed; }
</style>
