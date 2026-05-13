<template>
  <div class="my-booking">
    <!-- ═══ Nav ═══ -->
    <div class="mb-nav">
      <button class="mb-back" @click="goBack">
        <van-icon name="arrow-left" size="18" color="var(--ink-700)" />
      </button>
      <div class="mb-title">我的预约</div>
      <div style="width:36px;"></div>
    </div>

    <!-- 搜索（未自动登录时） -->
    <div v-if="showSearch" class="search-area">
      <div class="search-box">
        <van-icon name="phone-o" size="16" color="var(--pink-500)" style="flex-shrink:0;" />
        <input v-model="contactId" placeholder="输入联系方式" class="search-input" />
        <button class="search-btn" @click="searchReservations">查询</button>
      </div>
    </div>

    <!-- 切换账号 -->
    <div v-if="!showSearch && reservations.length > 0" class="switch-row">
      <button class="switch-btn" @click="showSearch = true; reservations = []; searched = false;">
        <van-icon name="exchange" size="12" /> 切换账号
      </button>
    </div>

    <!-- 加载 -->
    <van-loading v-if="searching" size="20px" vertical style="margin-top:80px;">加载中...</van-loading>

    <!-- ═══ 空状态 ═══ -->
    <div v-else-if="searched && reservations.length === 0" class="empty-state">
      <div class="empty-icon">🌷</div>
      <div class="empty-title">还没有预约</div>
      <div class="empty-sub">要来一次吗？</div>
      <div class="empty-spacer"></div>
      <button class="empty-btn" @click="router.push('/menu')">
        <van-icon name="calendar-o" size="16" style="margin-right:6px;" />
        开始预约
      </button>
    </div>

    <!-- ═══ 预约列表 ═══ -->
    <div v-else-if="reservations.length > 0" class="bk-list">
      <div v-for="r in reservations" :key="r.id" class="bk-card">
        <!-- 状态标签 + 订单号 -->
        <div class="bk-top">
          <span :class="['bk-badge', badgeSkin(r.status)]">{{ badgeLabel(r.status) }}</span>
          <span class="bk-order">B-{{ String(r.id).padStart(8, '0') }}</span>
        </div>

        <!-- 服务信息 -->
        <div class="bk-body">
          <div class="bk-thumb">💅</div>
          <div class="bk-info">
            <div class="bk-svc">美甲预约</div>
            <div class="bk-dt">{{ formatDate(r.reserveDate) }} {{ r.timeSlot }}</div>
          </div>
        </div>

        <!-- 费用 -->
        <div class="bk-footer">
          <div class="bk-fee-row">
            <span class="fk">服务总价</span>
            <span class="fv">¥{{ (r.totalAmount || 0).toLocaleString() }}</span>
          </div>
          <div class="bk-fee-row">
            <span class="fk">已付定金</span>
            <span class="fv" :class="depositColor(r.depositStatus)">¥{{ (r.depositAmount || 0).toLocaleString() }} · {{ depositLabel(r.depositStatus) }}</span>
          </div>
        </div>

        <!-- 操作 -->
        <div v-if="r.status === 'CONFIRMED' || r.status === 'PENDING_DEPOSIT'" class="bk-actions">
          <button v-if="r.depositStatus !== 'CUSTOMER_PAID' && r.depositStatus !== 'PAID'" class="ac-btn ac-cancel" @click="confirmCancel(r)">取消预约</button>
          <button class="ac-btn ac-route" @click="goRoute">路线</button>
        </div>
      </div>
    </div>

    <!-- 取消弹窗 -->
    <van-dialog v-model:show="showCancelDialog" title="取消预约" :show-cancel-button="true"
      confirm-button-color="var(--pink-500)" @confirm="doCancel">
      <p style="padding:0 20px;font-size:14px;color:var(--ink-700);line-height:1.6;">
        预约取消后定金不予退还。如需退款请联系店主微信。
      </p>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { showToast } from 'vant';
import { getReservationsByContact, userCancelReservation } from '../api';

const router = useRouter();
const goBack = () => router.back();
const goRoute = () => router.push('/guide');

const contactId = ref('');
const searching = ref(false);
const searched = ref(false);
const reservations = ref([]);
const showSearch = ref(true);

const showCancelDialog = ref(false);
const cancellingReservation = ref(null);

onMounted(() => {
  const stored = sessionStorage.getItem('userContactId');
  if (stored) {
    contactId.value = stored;
    showSearch.value = false;
    searchReservations();
  }
});

const formatDate = (d) => d ? `${parseInt(d.split('-')[1])}月${parseInt(d.split('-')[2])}日` : '';

const searchReservations = async () => {
  if (!contactId.value.trim()) { showToast('请输入联系方式'); return; }
  searching.value = true; searched.value = true;
  try {
    const res = await getReservationsByContact(contactId.value.trim());
    if (res.code === 200) reservations.value = res.data;
    else showToast(res.message || '查询失败');
  } catch { showToast('查询失败'); }
  finally { searching.value = false; }
};

const badgeSkin = (s) => {
  if (s === 'CONFIRMED' || s === 'BOOKED') return 'green';
  if (s === 'COMPLETED') return 'gray';
  if (s === 'CANCELLED') return 'red';
  return 'yellow'; // PENDING_DEPOSIT
};
const badgeLabel = (s) => ({
  PENDING_DEPOSIT: '待确认', CONFIRMED: '已确认', BOOKED: '已确认',
  COMPLETED: '已完成', CANCELLED: '已取消',
})[s] || s;

const depositLabel = (s) => ({
  NONE: '未付', CUSTOMER_PAID: '待确认', PAID: '已付',
  REFUNDED: '已退', FORFEITED: '已没收',
})[s] || s;

const depositColor = (s) => ({
  PAID: 'c-green', CUSTOMER_PAID: 'c-yellow',
  REFUNDED: 'c-gray', FORFEITED: 'c-gray',
})[s] || '';

const confirmCancel = (r) => { cancellingReservation.value = r; showCancelDialog.value = true; };
const doCancel = async () => {
  if (!cancellingReservation.value) return;
  try {
    const res = await userCancelReservation(cancellingReservation.value.id);
    if (res.code === 200) { showToast('已取消'); await searchReservations(); }
    else showToast(res.message || '取消失败');
  } catch { showToast('取消失败'); }
  cancellingReservation.value = null;
};
</script>

<style scoped>
.my-booking {
  min-height: 100vh;
  background: transparent;
  display: flex;
  flex-direction: column;
}

/* ─── Nav ─── */
.mb-nav {
  flex-shrink: 0;
  position: sticky; top: 0; z-index: 20;
  backdrop-filter: blur(18px) saturate(180%);
  background: rgba(253,243,245,0.78);
  border-bottom: 1px solid rgba(232,127,160,0.16);
  padding: 12px 14px; display: flex; align-items: center; gap: 10px;
}
.mb-back {
  width: 36px; height: 36px; border-radius: 999px;
  background: rgba(255,255,255,0.85); border: 1px solid var(--pink-200);
  display: grid; place-items: center; cursor: pointer;
  box-shadow: var(--shadow-1); flex-shrink: 0;
}
.mb-back:active { transform: scale(0.95); }
.mb-title { flex:1; text-align:center; font-family:var(--font-cjk); font-weight:800; color:var(--ink-900); font-size:15.5px; }

/* ─── Search ─── */
.search-area { padding: 14px 14px 0; }
.search-box {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px 8px 14px;
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
}
.search-input { flex:1; border:0; outline:0; font-family:var(--font-cjk); font-size:14px; color:var(--ink-900); background:transparent; }
.search-btn {
  padding: 6px 14px; border-radius: 999px; border:0;
  background: var(--pink-500); color:#fff; font-family:var(--font-body);
  font-size:13px; font-weight:600; cursor:pointer;
}
.switch-row { display:flex; justify-content:flex-end; padding:8px 14px 0; }
.switch-btn { background:transparent; border:0; font-size:12px; color:var(--pink-600); cursor:pointer; font-family:var(--font-cjk); }

/* ─── Empty ─── */
.empty-state {
  display:flex; flex-direction:column; align-items:center;
  flex: 1;
  padding-bottom: 40px;
}
.empty-icon { font-size:56px; margin-bottom:12px; margin-top:100px; }
.empty-title { font-family:var(--font-cjk); font-weight:700; font-size:20px; color:var(--ink-900); }
.empty-sub { font-family:var(--font-cjk); font-size:14px; color:var(--ink-500); margin-top:4px; }
.empty-spacer { flex:1; }
.empty-btn {
  width: calc(100% - 28px); padding: 14px;
  border-radius: 999px; border:0;
  background: linear-gradient(135deg, #ff6b9d 0%, #e8436e 100%);
  color:#fff; font-family:var(--font-cjk);
  font-size:16px; font-weight:700;
  cursor:pointer; display:flex; align-items:center; justify-content:center;
  box-shadow:0 4px 16px rgba(232,67,110,0.35);
}
.empty-btn:active { transform:scale(0.97); }

/* ─── Card ─── */
.bk-list { padding: 14px; display: flex; flex-direction: column; gap: 14px; }
.bk-card {
  background: var(--surface-frosted-strong);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--border-soft);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
  overflow: hidden;
}

/* Top row: badge + order */
.bk-top {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px 0;
}
.bk-badge {
  font-family: var(--font-body);
  font-weight: 700; font-size: 11px;
  padding: 3px 10px; border-radius: 999px;
  letter-spacing: 0.03em;
}
.bk-badge.green { background: #dbf0de; color: var(--accent-green); }
.bk-badge.gray  { background: var(--paper-100); color: var(--ink-500); }
.bk-badge.red   { background: #fde0e0; color: var(--accent-red); }
.bk-badge.yellow { background: #fef6e0; color: #b8860b; }
.bk-order {
  font-family: var(--font-body);
  font-size: 11px; color: var(--ink-400);
  letter-spacing: 0.02em;
}

/* Body: thumb + info */
.bk-body {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px;
}
.bk-thumb {
  width: 52px; height: 52px;
  border-radius: var(--radius-md);
  background: var(--pink-100);
  display: flex; align-items: center; justify-content: center;
  font-size: 24px; flex-shrink: 0;
}
.bk-info { flex:1; min-width:0; }
.bk-svc {
  font-family: var(--font-cjk); font-weight: 700;
  font-size: 14px; color: var(--ink-900);
}
.bk-dt {
  font-family: var(--font-cjk);
  font-size: 12px; color: var(--ink-500);
  margin-top: 2px;
}

/* Footer: pricing */
.bk-footer {
  padding: 0 16px 10px;
  border-bottom: 1px solid var(--border-soft);
}
.bk-fee-row {
  display: flex; justify-content: space-between;
  padding: 3px 0; font-size: 13px;
}
.fk { font-family: var(--font-cjk); color: var(--ink-500); }
.fv { font-family: var(--font-body); font-weight: 600; color: var(--ink-700); }
.c-green { color: var(--accent-green) !important; }
.c-yellow { color: #b8860b !important; }
.c-gray { color: var(--ink-400) !important; }

/* Actions */
.bk-actions {
  display: flex; gap: 8px;
  padding: 10px 16px 14px;
}
.ac-btn {
  flex: 1;
  padding: 9px 0;
  border-radius: 999px;
  border: 1px solid var(--border-soft);
  font-family: var(--font-cjk);
  font-size: 13px; font-weight: 600;
  cursor: pointer;
  transition: all var(--dur-base) var(--ease-soft);
}
.ac-btn:active { transform: scale(0.96); }
.ac-cancel {
  background: #fff; color: var(--accent-red);
  border-color: #fdd;
}
.ac-route {
  background: var(--pink-50); color: var(--pink-600);
  border-color: var(--pink-200);
}
</style>
