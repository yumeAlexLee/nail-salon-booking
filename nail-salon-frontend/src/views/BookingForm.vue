<template>
  <div class="booking-form">
    <van-nav-bar
      :title="$t('nav.fillInfo')"
      left-arrow
      @click-left="goBack"
      class="glass-effect" fixed placeholder :border="false"
    />
    
    <!-- ═══ 预约摘要 — AWAI frosted card ═══ -->
    <div class="summary-card">
      <div class="summary-title">{{ $t('form.summaryTitle') }}</div>
      <div class="summary-item">{{ $t('form.date') }}：{{ reserveDate }}</div>
      <div class="summary-item">{{ $t('form.time') }}：{{ timeSlot }}</div>
      <div class="summary-item">{{ $t('form.identity') }}：{{ customerType === 'NEW' ? $t('home.newCustomer') : $t('home.oldCustomer') }}</div>
    </div>

    <!-- ═══ 定金预付提醒 — AWAI frosted card ═══ -->
    <div class="deposit-notice">
      <div class="deposit-icon">💰</div>
      <div class="deposit-body">
        <div class="deposit-title">预约需预付定金</div>
        <div class="deposit-amount">¥500</div>
        <div class="deposit-desc">预约成功后请完成定金支付，到店后自动抵扣服务费用。</div>
      </div>
    </div>

    <van-form id="booking-form" @submit="onSubmit" class="booking-form-inner">
      <van-cell-group inset>
        <van-field
          v-model="form.name"
          name="name"
          :label="$t('form.nameLabel')"
          :placeholder="$t('form.namePlaceholder')"
          :rules="[{ required: true, message: $t('form.namePlaceholder') }]"
        />
        <van-field
          v-model="form.contactId"
          name="contactId"
          :label="$t('form.contactLabel')"
          :placeholder="$t('form.contactPlaceholder')"
          :rules="[{ required: true, message: $t('form.contactPlaceholder') }]"
        />
        <van-field name="removalType" :label="$t('form.removalLabel')">
          <template #input>
            <van-radio-group v-model="form.removalType" direction="horizontal">
              <van-radio name="本甲">{{ $t('form.removalType.natural') }}</van-radio>
              <van-radio name="甲片">{{ $t('form.removalType.extension') }}</van-radio>
              <van-radio name="无">{{ $t('form.removalType.none') }}</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
          v-model="form.remarks"
          rows="2"
          autosize
          :label="$t('form.remarksLabel')"
          type="textarea"
          maxlength="50"
          :placeholder="$t('form.remarksPlaceholder')"
          show-word-limit
        />
        <van-field name="uploader" :label="$t('form.imageLabel')">
          <template #input>
            <van-uploader v-model="fileList" :after-read="afterRead" :max-count="1" />
          </template>
        </van-field>
      </van-cell-group>
    </van-form>
    
    <div class="bottom-action glass-effect">
      <van-button round block type="primary" native-type="submit" form="booking-form" :loading="loading" class="apple-btn-active" size="large">
        {{ $t('form.submitBtn') }}
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { submitReservation, uploadImage } from '../api';
import { showDialog } from 'vant';

const router = useRouter();
const { t } = useI18n();

const customerType = ref('');
const reserveDate = ref('');
const timeSlot = ref('');
const loading = ref(false);

const form = ref({
  name: sessionStorage.getItem('userName') || '',
  contactId: sessionStorage.getItem('userContactId') || '',
  removalType: '本甲',
  remarks: '',
  referenceImage: ''
});

const fileList = ref([]);

const afterRead = async (file) => {
  file.status = 'uploading';
  file.message = t('form.uploading');
  
  try {
    const formData = new FormData();
    formData.append('file', file.file);
    const res = await uploadImage(formData);
    
    if (res.code === 200) {
      file.status = 'done';
      form.value.referenceImage = res.data;
    } else {
      file.status = 'failed';
      file.message = t('form.uploadFailed');
    }
  } catch (error) {
    file.status = 'failed';
    file.message = t('form.uploadFailed');
  }
};

onMounted(() => {
  customerType.value = sessionStorage.getItem('customerType') || '新客';
  reserveDate.value = sessionStorage.getItem('reserveDate');
  timeSlot.value = sessionStorage.getItem('timeSlot');
  
  if (!reserveDate.value || !timeSlot.value) {
    router.replace('/');
  }
});

const goBack = () => router.back();

const onSubmit = async () => {
  loading.value = true;
  try {
    const data = {
      ...form.value,
      customerType: customerType.value,
      reserveDate: reserveDate.value,
      timeSlot: timeSlot.value
    };
    const res = await submitReservation(data);
    if (res.code === 200) {
      // 保存预约 ID，供支付页面使用
      if (res.data) {
        sessionStorage.setItem('lastReservationId', String(res.data));
      }
      router.replace('/success');
    } else {
      showDialog({
        title: t('form.submitFailed'),
        message: res.message,
      });
    }
  } catch (error) {
    // 处理后端抛出的400或409错误
    if(error.response && error.response.data && error.response.data.message) {
      showDialog({
        title: 'Info',
        message: error.response.data.message,
      });
    } else {
      showDialog({
        title: 'Error',
        message: t('form.networkError'),
      });
    }
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
/* ─── Page shell ──────────────────────────────────── */
.booking-form {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding-bottom: 0;
}

/* ═══ 预约摘要 — AWAI frosted card ═══ */
.summary-card {
  margin: var(--space-4);
  padding: var(--space-5);
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-2);
}
.summary-title {
  font-size: var(--fs-18);
  font-weight: 600;
  margin-bottom: var(--space-4);
  color: var(--fg-1);
  border-bottom: 1px solid var(--border-soft);
  padding-bottom: var(--space-3);
  font-family: var(--font-cjk);
}
.summary-item {
  font-size: var(--fs-15);
  color: var(--fg-2);
  line-height: 1.8;
  display: flex;
  justify-content: space-between;
}

/* ═══ 定金提醒 — AWAI frosted card ═══ */
.deposit-notice {
  margin: var(--space-3) var(--space-4);
  padding: var(--space-4);
  background: var(--surface-frosted);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-md);
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}
.deposit-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}
.deposit-body {
  flex: 1;
}
.deposit-title {
  font-size: var(--fs-15);
  font-weight: 600;
  color: var(--pink-600);
  margin-bottom: var(--space-1);
  font-family: var(--font-cjk);
}
.deposit-amount {
  font-size: var(--fs-24);
  font-weight: 800;
  color: var(--pink-500);
  margin-bottom: var(--space-1);
}
.deposit-desc {
  font-size: var(--fs-13);
  color: var(--fg-3);
  line-height: 1.5;
}

/* ─── Vant overrides ──────────────────────────────── */
:deep(.van-cell-group--inset) {
  margin: var(--space-4);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
:deep(.van-field__label) {
  color: var(--fg-1);
  font-weight: 500;
}

/* ─── Bottom action bar ───────────────────────────── */
.bottom-action {
  position: sticky;
  bottom: 0;
  margin-top: auto;
  padding: var(--space-4) var(--space-6) var(--space-8) var(--space-6);
  border-top: 1px solid var(--border-soft);
  z-index: 100;
}

/* ─── Form layout ─────────────────────────────────── */
.booking-form-inner {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.booking-form-inner > .van-cell-group {
  margin-top: 0;
}
.bottom-action .van-button {
  font-weight: 600;
}
</style>
