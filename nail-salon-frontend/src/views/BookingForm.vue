<template>
  <div class="booking-form">
    <van-nav-bar
      :title="$t('nav.fillInfo')"
      left-arrow
      @click-left="goBack"
      class="glass-effect" fixed placeholder :border="false"
    />
    
    <div class="summary-card">
      <div class="summary-title">{{ $t('form.summaryTitle') }}</div>
      <div class="summary-item">{{ $t('form.date') }}：{{ reserveDate }}</div>
      <div class="summary-item">{{ $t('form.time') }}：{{ timeSlot }}</div>
      <div class="summary-item">{{ $t('form.identity') }}：{{ customerType === 'NEW' ? $t('home.newCustomer') : $t('home.oldCustomer') }}</div>
    </div>

    <!-- ─── 定金预付提醒 ─────────────────────────────── -->
    <div class="deposit-notice">
      <div class="deposit-icon">💰</div>
      <div class="deposit-body">
        <div class="deposit-title">预约需预付定金</div>
        <div class="deposit-amount">¥500</div>
        <div class="deposit-desc">预约成功后请完成定金支付，到店后自动抵扣服务费用。</div>
      </div>
    </div>
    <!-- ─────────────────────────────────────────────── -->

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
.booking-form {
  min-height: 100vh;
  background-color: var(--apple-bg);
  display: flex;
  flex-direction: column;
  padding-bottom: 0;
}
.summary-card {
  margin: 16px;
  padding: 20px;
  background-color: var(--apple-white);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
}
.summary-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--apple-text);
  border-bottom: 1px solid rgba(0,0,0,0.05);
  padding-bottom: 12px;
}
.summary-item {
  font-size: 15px;
  color: var(--apple-text-secondary);
  line-height: 1.8;
  display: flex;
  justify-content: space-between;
}

/* ─── 定金提醒卡片样式 ─────────────────────────── */
.deposit-notice {
  margin: 12px 16px;
  padding: 16px;
  background: linear-gradient(135deg, #fff7e6 0%, #fff3d6 100%);
  border: 1px solid #ffe0a0;
  border-radius: 14px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
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
  font-size: 15px;
  font-weight: 600;
  color: #7a5800;
  margin-bottom: 4px;
}
.deposit-amount {
  font-size: 22px;
  font-weight: 700;
  color: #d48806;
  margin-bottom: 4px;
}
.deposit-desc {
  font-size: 13px;
  color: #a67c00;
  line-height: 1.5;
}
/* ─────────────────────────────────────────────── */

:deep(.van-cell-group--inset) {
  margin: 16px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.04);
  overflow: hidden;
}
:deep(.van-field__label) {
  color: var(--apple-text);
  font-weight: 500;
}
.bottom-action {
  position: sticky;
  bottom: 0;
  margin-top: auto;
  padding: 16px 24px 32px 24px;
  border-top: 1px solid rgba(0,0,0,0.05);
  z-index: 100;
}
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
