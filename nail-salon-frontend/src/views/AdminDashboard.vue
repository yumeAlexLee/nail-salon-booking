<template>
  <div class="admin-page">
    <van-nav-bar title="店主排期表" />

    <!-- 密码验证区域 -->
    <div v-if="!isAuthenticated" class="login-box">
      <h2 class="title">验证身份</h2>
      <van-cell-group inset>
        <van-field
          v-model="password"
          type="password"
          label="后台口令"
          placeholder="请输入口令 (888888)"
        />
      </van-cell-group>
      <div style="margin: 16px;">
        <van-button round block type="primary" @click="checkPassword">进入大盘</van-button>
      </div>
    </div>

    <!-- 数据看板区域 -->
    <div v-else class="dashboard-content">
      
      <!-- 视图切换 -->
      <div class="tabs-wrapper">
        <van-tabs v-model:active="currentView" type="card" color="#1989fa">
          <van-tab title="未来 7 天" name="week"></van-tab>
          <van-tab title="单日排班" name="day"></van-tab>
          <van-tab title="已取消" name="cancelled"></van-tab>
          <van-tab title="菜单管理" name="menu"></van-tab>
          <van-tab title="店铺设置" name="settings"></van-tab>
        </van-tabs>
      </div>

      <van-loading v-if="loading" size="24px" vertical style="margin-top: 50px;">加载中...</van-loading>

      <!-- 周视图 (概览) -->
      <div v-show="currentView === 'week' && !loading" class="week-view">
        <div class="summary-tip">💡 点击以下任意一天，可直接进入当天的排期表</div>
        <van-cell-group inset class="week-list">
          <van-cell v-for="day in weekSummary" :key="day.date" 
                    is-link 
                    size="large"
                    center
                    @click="goToDayView(day.date)">
            <template #title>
              <div class="day-title">
                <span class="day-date">{{ day.displayDate }}</span>
                <span class="day-week">{{ day.weekday }}</span>
                <van-tag v-if="day.isToday" type="danger" size="small" style="margin-left: 8px;">今天</van-tag>
              </div>
            </template>
            <template #value>
              <van-tag :type="day.available === 0 ? 'danger' : (day.available <= 2 ? 'warning' : 'success')" size="medium">
                {{ day.available === 0 ? '全部排满' : `剩 ${day.available} 个空位` }}
              </van-tag>
            </template>
          </van-cell>
        </van-cell-group>
      </div>

      <!-- 日视图 (详情日程表) -->
      <div v-show="currentView === 'day' && !loading" class="schedule-container">
        <!-- 日期切换条 -->
        <div class="date-picker-bar">
          <van-button icon="arrow-left" size="small" type="default" @click="changeDate(-1)" />
          <div class="current-date" @click="showCalendar = true">
            <van-icon name="calendar-o" />
            {{ selectedDate }}
          </div>
          <van-button icon="arrow" size="small" type="default" @click="changeDate(1)" />
        </div>
        
        <div class="day-actions" style="display: flex; gap: 10px; margin-bottom: 15px;">
          <van-button icon="lock" type="danger" size="small" plain block @click="handleLockDay">锁定全天</van-button>
          <van-button icon="unlock" type="primary" size="small" plain block @click="handleUnlockDay">解锁全天</van-button>
        </div>

        <van-calendar v-model:show="showCalendar" @confirm="onCalendarConfirm" :show-confirm="false" />

        <!-- 时间轴主体 -->
        <div class="timeline">
          <div v-for="slot in ALL_SLOTS" :key="slot" class="time-block-wrapper">
            <div class="time-label">{{ slot.split('-')[0] }}</div>
            
            <!-- 状态：已预约 -->
            <div v-if="getSlotStatus(slot) === 'BOOKED'" 
                 class="slot-card booked" 
                 :class="{ 'new-customer': getSlotData(slot).customerType === 'NEW' }"
                 @click="showDetails(getSlotData(slot))">
              <div class="slot-header">
                <span class="name">{{ getSlotData(slot).name }}</span>
                <van-tag :type="getSlotData(slot).customerType === 'NEW' ? 'primary' : 'success'" plain>
                  {{ getSlotData(slot).customerType === 'NEW' ? '新客' : '老客' }}
                </van-tag>
              </div>
              <div class="slot-summary">
                卸甲: {{ getSlotData(slot).removalType }}
                <van-tag v-if="getSlotData(slot).depositStatus === 'PAID'" type="success" size="small" style="margin-left:4px;">定金✅</van-tag>
                <van-tag v-else-if="getSlotData(slot).depositStatus === 'PENDING_REFUND'" type="warning" size="small" style="margin-left:4px;">退款中⚠️</van-tag>
                <van-tag v-else-if="getSlotData(slot).depositStatus === 'NONE'" type="default" size="small" style="margin-left:4px;">未付定金</van-tag>
              </div>
            </div>

            <!-- 状态：店主手动锁定 -->
            <div v-else-if="getSlotStatus(slot) === 'LOCKED'" 
                 class="slot-card locked" 
                 @click="handleUnlock(slot)">
              <van-icon name="lock" /> 店主已锁单 (点击解锁)
            </div>

            <!-- 状态：空闲可预约 -->
            <div v-else 
                 class="slot-card available" 
                 @click="handleLock(slot)">
              <van-icon name="add-o" /> 空闲 (点击可锁单)
            </div>
          </div>
        </div>
      </div>

      <!-- 已取消预约视图（可恢复） -->
      <div v-show="currentView === 'cancelled' && !loading" class="cancelled-view">
        <div class="summary-tip" v-if="cancelledReservations.length === 0">暂无已取消的预约</div>
        <van-cell-group inset v-else class="cancelled-list">
          <van-cell v-for="r in cancelledReservations" :key="r.id" size="large">
            <template #title>
              <div class="cancelled-item">
                <div class="cancelled-name">{{ r.name }}</div>
                <div class="cancelled-meta">{{ r.reserveDate }} {{ r.timeSlot }}</div>
                <div class="cancelled-meta">{{ r.contactId }} · {{ r.removalType }}</div>
              </div>
            </template>
            <template #value>
              <van-button size="small" type="primary" plain @click="handleRestore(r.id, r.name)">
                恢复预约
              </van-button>
            </template>
          </van-cell>
        </van-cell-group>
      </div>

      <!-- 菜单管理视图 -->
      <div v-show="currentView === 'menu' && !loading" class="menu-mgmt-view">
        <div class="menu-mgmt-header">
          <van-button type="primary" size="small" icon="plus" @click="showMenuEditor(null)">添加项目</van-button>
        </div>

        <div v-for="group in menuGroups" :key="group.category" class="menu-group">
          <div class="menu-group-title">{{ group.displayName }}</div>
          <van-cell-group inset>
            <van-cell v-for="item in group.items" :key="item.id" class="menu-item-cell"
                      is-link @click="showMenuEditor(item)">
              <template #title>
                <div class="menu-item-name">
                  <span class="item-name">{{ item.name }}</span>
                  <span v-if="item.nameJa" class="item-name-ja"> / {{ item.nameJa }}</span>
                </div>
                <div class="menu-item-meta">
                  <span class="item-price">¥{{ item.price.toLocaleString() }}</span>
                  <span v-if="item.duration" class="item-duration"> · {{ item.duration }}分</span>
                </div>
                <van-button
                  size="mini" plain type="primary"
                  style="margin-top:6px;"
                  @click.stop="showOptionsEditor(item)"
                >
                  选项管理
                </van-button>
              </template>
              <template #value>
                <van-switch v-model="item.isActive" size="20" @click.stop @change="(v) => toggleActive(item, v)"
                            :active-value="1" :inactive-value="0" />
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </div>

      <!-- 菜单编辑弹窗 -->
      <van-popup v-model:show="showMenuPopup" position="right" :style="{ width: '90%', height: '100%' }" :overlay-style="{ backgroundColor: 'rgba(0,0,0,0.4)' }">
        <div class="menu-editor">
          <h3 class="popup-title">{{ editingItem ? '编辑项目' : '新增项目' }}</h3>
          <van-form @submit="saveMenuItem">
            <van-cell-group inset>
              <van-field label="分类(中)" v-model="menuForm.category" placeholder="如: 美甲" :rules="[{required:true}]" />
              <van-field label="分类(日)" v-model="menuForm.categoryJa" placeholder="如: ネイル" />
              <van-field label="名称(中)" v-model="menuForm.name" placeholder="项目名称" :rules="[{required:true}]" />
              <van-field label="名称(日)" v-model="menuForm.nameJa" placeholder="日文名称" />
              <van-field label="价格" v-model="menuForm.price" type="digit" placeholder="日元" :rules="[{required:true}]" />
              <van-field label="时长(分)" v-model="menuForm.duration" type="digit" placeholder="如: 60" />
              <van-field label="排序" v-model="menuForm.sortOrder" type="digit" placeholder="数字越小越靠前" />
              <van-field label="示意图URL" v-model="menuForm.imageUrl" placeholder="例如: /uploads/menu/xxx.jpg" />
              <van-field label="说明(中)" v-model="menuForm.description" type="textarea" rows="2" />
              <van-field label="说明(日)" v-model="menuForm.descriptionJa" type="textarea" rows="2" />
            </van-cell-group>
            <div v-if="editingItem" style="margin: 8px 16px;">
              <van-button plain block type="danger" size="small" @click="handleDeleteMenu(editingItem.id, editingItem.name)">删除此项目</van-button>
            </div>
            <div style="margin: 16px;">
              <van-button round block type="primary" @click="saveMenuItem">保存</van-button>
            </div>
          </van-form>
        </div>
      </van-popup>

      <!-- 子选项编辑弹窗 -->
      <van-popup v-model:show="showOptionPopup" position="right"
        :style="{ width: '90%', height: '100%' }"
        :overlay-style="{ backgroundColor: 'rgba(0,0,0,0.4)' }"
        @closed="optionEditingItem = null"
      >
        <div class="menu-editor" style="overflow-y:auto;height:100%;">
          <h3 class="popup-title">
            {{ optionEditingItem?.name || '子选项管理' }}
            <span v-if="optionEditingItem" style="font-size:13px;color:#999;"> 的附加选项</span>
          </h3>

          <!-- 现有选项列表 -->
          <div v-if="optionsList.length > 0" style="margin-bottom:16px;padding:0 16px;">
            <div v-for="opt in optionsList" :key="opt.id"
              style="display:flex;align-items:center;padding:10px 0;border-bottom:1px solid #f0f0f0;">
              <div style="flex:1;">
                <div style="font-weight:600;font-size:14px;">{{ opt.name }}</div>
                <div style="font-size:12px;color:#999;">
                  <span v-if="opt.nameJa">{{ opt.nameJa }} · </span>
                  +¥{{ opt.price.toLocaleString() }}
                  <span v-if="opt.duration"> · +{{ opt.duration }}分</span>
                </div>
              </div>
              <van-button size="mini" plain @click.stop="editOption(opt)" style="margin-right:6px;">编辑</van-button>
              <van-button size="mini" plain type="danger" @click.stop="handleDeleteOption(opt)">删除</van-button>
            </div>
          </div>
          <div v-else style="text-align:center;color:#969799;padding:20px 0;font-size:14px;">暂无选项</div>

          <!-- 添加/编辑表单 -->
          <van-form @submit="saveOption">
            <van-cell-group inset>
              <van-field label="名称(中)" v-model="optionForm.name" placeholder="如: 贴钻" :rules="[{required:true}]" />
              <van-field label="名称(日)" v-model="optionForm.nameJa" placeholder="如: ストーン" />
              <van-field label="追加价格" v-model="optionForm.price" type="digit" placeholder="日元" :rules="[{required:true}]" />
              <van-field label="追加时长(分)" v-model="optionForm.duration" type="digit" placeholder="如: 15" />
              <van-field label="排序" v-model="optionForm.sortOrder" type="digit" placeholder="数字越小越靠前" />
            </van-cell-group>
            <div style="margin:16px;">
              <van-button round block type="primary" @click="saveOption">
                {{ optionForm.id ? '保存修改' : '添加选项' }}
              </van-button>
              <van-button v-if="optionForm.id" round plain block style="margin-top:8px;" @click="cancelOptionEdit">
                取消编辑
              </van-button>
            </div>
          </van-form>
        </div>
      </van-popup>

      <!-- 店铺设置视图 -->
      <div v-show="currentView === 'settings' && !loading" class="settings-view">
        <van-cell-group inset title="营业时间配置">
          <van-field
            v-model="storeSettings.open_time"
            label="开门时间"
            placeholder="例如: 10:00"
          />
          <van-field
            v-model="storeSettings.close_time"
            label="关门时间"
            placeholder="例如: 20:00"
          />
        </van-cell-group>
        <div class="summary-tip" style="margin-top: 10px;">
          设置后，系统会自动按照2小时一格切分排班时间。例如：10:00到20:00会生成5个排班格子。
        </div>

        <van-cell-group inset title="定金配置" style="margin-top: 16px;">
          <van-field
            v-model="storeSettings.deposit_amount"
            label="默认定金(日元)"
            type="digit"
            placeholder="例如: 1000"
          />
        </van-cell-group>
        <div class="summary-tip">
          客户预约时需支付定金，店主可在预约详情中标记收款/退款/没收。
        </div>

        <div style="margin: 16px;">
          <van-button round block type="primary" @click="saveSettings">保存配置并生效</van-button>
        </div>
      </div>
    </div>

    <!-- 预约详情弹窗 -->
    <van-popup v-model:show="showPopup" round position="bottom" :style="{ height: '40%' }">
      <div class="popup-content" v-if="currentReservation">
        <h3 class="popup-title">预约详情</h3>
        <van-cell-group>
          <van-cell title="预约时间" :value="`${currentReservation.reserveDate} ${currentReservation.timeSlot}`" />
          <van-cell title="客人姓名" :value="currentReservation.name" />
          <van-cell title="客人身份">
            <template #right-icon>
              <van-tag :type="currentReservation.customerType === 'NEW' ? 'primary' : 'success'">
                {{ currentReservation.customerType === 'NEW' ? '新客' : '老客' }}
              </van-tag>
            </template>
          </van-cell>
          <van-cell title="联系方式" :value="currentReservation.contactId" />
          <van-cell title="卸甲类型" :value="currentReservation.removalType" />
          <van-cell title="客人备注" :label="currentReservation.remarks || '无'" />
        </van-cell-group>

        <!-- 定金信息 -->
        <van-cell-group inset style="margin-top:12px;">
          <van-cell title="定金">
            <template #value>
              <van-tag :type="depositTagType(currentReservation.depositStatus)" size="medium">
                {{ depositStatusLabel(currentReservation) }}
              </van-tag>
            </template>
          </van-cell>
        </van-cell-group>
        <div style="display:flex; gap:8px; margin: 8px 16px; flex-wrap:wrap;" v-if="currentReservation.depositStatus === 'NONE'">
          <van-button size="small" type="primary" plain @click="handleDepositPaid(currentReservation.id)">💰 标记已收款</van-button>
        </div>
        <div style="display:flex; gap:8px; margin: 8px 16px; flex-wrap:wrap;" v-if="currentReservation.depositStatus === 'CUSTOMER_PAID'">
          <van-button size="small" type="primary" @click="handleDepositPaid(currentReservation.id)">💰 确认收款</van-button>
        </div>
        <div style="display:flex; gap:8px; margin: 8px 16px; flex-wrap:wrap;" v-if="currentReservation.depositStatus === 'PAID'">
          <van-button size="small" type="warning" plain @click="handleDepositRefunded(currentReservation.id)">↩️ 退款</van-button>
          <van-button size="small" type="danger" plain @click="handleForfeitDeposit(currentReservation.id)">🚫 没收 (No-show)</van-button>
        </div>
        <div style="display:flex; gap:8px; margin: 8px 16px; flex-wrap:wrap;" v-if="currentReservation.depositStatus === 'PENDING_REFUND'">
          <van-button size="small" type="success" plain @click="handleDepositRefunded(currentReservation.id)">✅ 已手动退款</van-button>
          <van-button size="small" type="danger" plain @click="handleForfeitDeposit(currentReservation.id)">🚫 没收 (不退款)</van-button>
        </div>
        
        <div v-if="currentReservation.referenceImage" class="reference-image-box">
          <p class="img-title">客人上传的参考款式图：</p>
          <img :src="currentReservation.referenceImage" class="ref-img" alt="款式参考" />
        </div>

        <div style="margin: 16px;">
          <van-button plain block type="danger" @click="handleCancelReservation(currentReservation)">
            取消该预约 (释放此时间段)
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { showToast, showConfirmDialog } from 'vant';
import { getAdminReservations, getAdminLockedSlots, lockSlot, unlockSlot, deleteReservation, getSettings, updateSettings, lockDay, unlockDay, getCancelledReservations, restoreReservation, getAllMenuItems, createMenuItem, updateMenuItem, deleteMenuItem, markDepositPaid, markDepositRefunded, forfeitDeposit, getAdminMenuOptions, createMenuOption, updateMenuOption, deleteMenuOption } from '../api';

// 认证与基础状态
const password = ref('');
const isAuthenticated = ref(false);
const SECRET_CODE = '888888';
const loading = ref(false);

// 视图切换控制
const currentView = ref('week'); // 'week' 或 'day'

// 数据存储
const allReservations = ref([]);
const allLockedSlots = ref([]);
const cancelledReservations = ref([]);
const allMenuItems = ref([]);
const showMenuPopup = ref(false);
const editingItem = ref(null);

// ─── 子选项管理 ───
const showOptionPopup = ref(false);
const optionEditingItem = ref(null);
const optionsList = ref([]);
const optionForm = ref({ name: '', nameJa: '', price: '', duration: '', sortOrder: '' });
const menuForm = ref({
  category: '', categoryJa: '', name: '', nameJa: '',
  price: '', duration: '', sortOrder: '', description: '', descriptionJa: ''
});
const ALL_SLOTS = ref([]);
const storeSettings = ref({ open_time: '10:00', close_time: '20:00', deposit_amount: '1000' });

// 菜单项目按分类分组，用于展示
const menuGroups = computed(() => {
  const map = {};
  for (const item of allMenuItems.value) {
    const key = item.category;
    if (!map[key]) map[key] = { category: key, displayName: item.categoryJa ? `${key}（${item.categoryJa}）` : key, items: [] };
    map[key].items.push(item);
  }
  return Object.values(map);
});

const generateSlots = () => {
  const slots = [];
  let [h, m] = storeSettings.value.open_time.split(':').map(Number);
  let [ch, cm] = storeSettings.value.close_time.split(':').map(Number);
  
  let currentMins = h * 60 + m;
  const closeMins = ch * 60 + cm;
  
  while (currentMins + 120 <= closeMins) {
    const endMins = currentMins + 120;
    const startStr = `${String(Math.floor(currentMins/60)).padStart(2,'0')}:${String(currentMins%60).padStart(2,'0')}`;
    const endStr = `${String(Math.floor(endMins/60)).padStart(2,'0')}:${String(endMins%60).padStart(2,'0')}`;
    slots.push(`${startStr}-${endStr}`);
    currentMins += 120;
  }
  ALL_SLOTS.value = slots;
};

// 日期相关
const getLocalDateString = (date) => {
  const d = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return d.toISOString().split('T')[0];
};

const todayStr = getLocalDateString(new Date());
const selectedDate = ref(todayStr);
const showCalendar = ref(false);

// 弹窗状态
const showPopup = ref(false);
const currentReservation = ref(null);

// 登录与拉取数据
const checkPassword = () => {
  if (password.value === SECRET_CODE) {
    isAuthenticated.value = true;
    sessionStorage.setItem('admin_auth', 'true');
    fetchAllData();
  } else {
    showToast('口令错误');
  }
};

const fetchAllData = async () => {
  loading.value = true;
  try {
    const resSettings = await getSettings();
    if (resSettings.code === 200 && Object.keys(resSettings.data).length > 0) {
      storeSettings.value.open_time = resSettings.data.open_time || '10:00';
      storeSettings.value.close_time = resSettings.data.close_time || '20:00';
      storeSettings.value.deposit_amount = resSettings.data.deposit_amount || '1000';
    }
    generateSlots(); // 根据最新设置生成排班

    const resReservations = await getAdminReservations();
    const resLocked = await getAdminLockedSlots();
    if (resReservations.code === 200) allReservations.value = resReservations.data;
    if (resLocked.code === 200) allLockedSlots.value = resLocked.data;
    
    // 加载已取消的预约
    const resCancelled = await getCancelledReservations();
    if (resCancelled.code === 200) cancelledReservations.value = resCancelled.data;
    
    // 加载菜单项目
    const resMenu = await getAllMenuItems();
    if (resMenu.code === 200) allMenuItems.value = resMenu.data;
  } catch (error) {
    showToast('数据加载失败');
  } finally {
    loading.value = false;
  }
};

const saveSettings = async () => {
  loading.value = true;
  try {
    const data = {
      open_time: storeSettings.value.open_time,
      close_time: storeSettings.value.close_time,
      deposit_amount: storeSettings.value.deposit_amount || '1000'
    };
    await updateSettings(data);
    showToast('设置保存成功');
    generateSlots();
  } catch (error) {
    showToast('设置保存失败');
  } finally {
    loading.value = false;
  }
};

// ================= 周视图逻辑 =================
const weekSummary = computed(() => {
  const summary = [];
  const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  
  // 生成从今天起未来 7 天的数据
  for (let i = 0; i < 7; i++) {
    const targetDate = new Date();
    targetDate.setDate(targetDate.getDate() + i);
    const dateStr = getLocalDateString(targetDate);
    
    // 统计当天的预约数和锁定数（排除已取消的记录）
    const bookedCount = allReservations.value.filter(r =>
        r.reserveDate === dateStr && r.status !== 'CANCELLED'
    ).length;
    const lockedCount = allLockedSlots.value.filter(l => l.lockDate === dateStr).length;
    const availableCount = ALL_SLOTS.value.length - bookedCount - lockedCount;

    summary.push({
      date: dateStr,
      displayDate: `${targetDate.getMonth() + 1}月${targetDate.getDate()}日`,
      weekday: weekdays[targetDate.getDay()],
      isToday: i === 0,
      available: availableCount
    });
  }
  return summary;
});

const goToDayView = (dateStr) => {
  selectedDate.value = dateStr;
  currentView.value = 'day';
};

// ================= 日视图逻辑 =================
const changeDate = (days) => {
  const date = new Date(selectedDate.value);
  date.setDate(date.getDate() + days);
  selectedDate.value = getLocalDateString(date);
};

const onCalendarConfirm = (date) => {
  selectedDate.value = getLocalDateString(date);
  showCalendar.value = false;
};

const getSlotStatus = (timeSlot) => {
  const dateStr = selectedDate.value;
  const booking = allReservations.value.find(r =>
      r.reserveDate === dateStr && r.timeSlot === timeSlot && r.status !== 'CANCELLED'
  );
  if (booking) return 'BOOKED';
  const locked = allLockedSlots.value.find(l => l.lockDate === dateStr && l.timeSlot === timeSlot);
  if (locked) return 'LOCKED';
  return 'AVAILABLE';
};

const getSlotData = (timeSlot) => {
  return allReservations.value.find(r => r.reserveDate === selectedDate.value && r.timeSlot === timeSlot);
};

// 操作事件
const showDetails = (reservation) => {
  currentReservation.value = reservation;
  showPopup.value = true;
};

const handleLock = (timeSlot) => {
  showConfirmDialog({
    title: '锁定时间段',
    message: `锁定 ${selectedDate.value} ${timeSlot} 后，客人将无法在网页上预约该时段。`,
  }).then(async () => {
    loading.value = true;
    try {
      await lockSlot(selectedDate.value, timeSlot);
      showToast('锁定成功');
      await fetchAllData();
    } catch (e) {
      showToast('锁定失败');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

const handleUnlock = (timeSlot) => {
  showConfirmDialog({
    title: '解除锁定',
    message: `确认释放 ${selectedDate.value} ${timeSlot} 给客人预约吗？`,
  }).then(async () => {
    loading.value = true;
    try {
      await unlockSlot(selectedDate.value, timeSlot);
      showToast('已解除锁定');
      await fetchAllData();
    } catch (e) {
      showToast('解除失败');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

const handleLockDay = () => {
  showConfirmDialog({
    title: '锁定全天',
    message: `确定要将 ${selectedDate.value} 设为休息日吗？客将无法预约全天任何时段。`,
  }).then(async () => {
    loading.value = true;
    try {
      await lockDay(selectedDate.value);
      showToast('已全天锁定');
      await fetchAllData();
    } catch (e) {
      showToast('全天锁定失败');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

const handleUnlockDay = () => {
  showConfirmDialog({
    title: '解锁全天',
    message: `确定要解锁 ${selectedDate.value} 的所有时间段吗？`,
  }).then(async () => {
    loading.value = true;
    try {
      await unlockDay(selectedDate.value);
      showToast('已解锁全天');
      await fetchAllData();
    } catch (e) {
      showToast('解锁全天失败');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

const handleCancelReservation = (reservation) => {
  showConfirmDialog({
    title: '取消预约',
    message: `确定要取消客人【${reservation.name}】的这笔预约吗？取消后该时间段将释放给其他客户。如需恢复，可在「已取消」标签页中操作。`,
    confirmButtonColor: 'var(--accent-red)'
  }).then(async () => {
    loading.value = true;
    try {
      await deleteReservation(reservation.id);
      showToast('取消成功');
      showPopup.value = false;
      await fetchAllData();
    } catch (e) {
      showToast('取消失败');
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

// 恢复已取消的预约（仅老客可恢复）
const handleRestore = (id, name) => {
  showConfirmDialog({
    title: '恢复预约',
    message: `确定要恢复客人【${name}】的预约吗？恢复后该时间段将重新为此客户锁定。`,
    confirmButtonColor: 'var(--accent-green)'
  }).then(async () => {
    loading.value = true;
    try {
      const res = await restoreReservation(id);
      if (res.code === 200) {
        showToast('恢复成功');
        await fetchAllData();
      } else {
        showToast(res.message || '恢复失败');
      }
    } catch (e) {
      const msg = e?.response?.data?.message || '恢复失败：该客户无历史预约记录';
      showToast(msg);
    } finally {
      loading.value = false;
    }
  }).catch(() => {});
};

// ─── 菜单管理方法 ──────────────────────────────────

const showMenuEditor = (item) => {
  editingItem.value = item;
  if (item) {
    menuForm.value = {
      category: item.category || '', categoryJa: item.categoryJa || '',
      name: item.name || '', nameJa: item.nameJa || '',
      price: String(item.price || ''),
      duration: String(item.duration || ''),
      sortOrder: String(item.sortOrder || ''),
      imageUrl: item.imageUrl || '',
      description: item.description || '',
      descriptionJa: item.descriptionJa || ''
    };
  } else {
    menuForm.value = { category: '', categoryJa: '', name: '', nameJa: '', price: '', duration: '', sortOrder: '', imageUrl: '', description: '', descriptionJa: '' };
  }
  showMenuPopup.value = true;
};

const saveMenuItem = async () => {
  const data = {
    category: menuForm.value.category,
    categoryJa: menuForm.value.categoryJa,
    name: menuForm.value.name,
    nameJa: menuForm.value.nameJa,
    price: parseInt(menuForm.value.price),
    duration: menuForm.value.duration ? parseInt(menuForm.value.duration) : null,
    sortOrder: menuForm.value.sortOrder ? parseInt(menuForm.value.sortOrder) : 0,
    imageUrl: menuForm.value.imageUrl || null,
    description: menuForm.value.description,
    descriptionJa: menuForm.value.descriptionJa
  };

  try {
    if (editingItem.value) {
      await updateMenuItem(editingItem.value.id, data);
      showToast('更新成功');
    } else {
      await createMenuItem(data);
      showToast('添加成功');
    }
    showMenuPopup.value = false;
    await fetchAllData();
  } catch (e) {
    showToast('操作失败');
  }
};

const toggleActive = async (item, value) => {
  try {
    await updateMenuItem(item.id, { ...item, isActive: value });
  } catch (e) {
    showToast('操作失败');
  }
};

const handleDeleteMenu = (id, name) => {
  showConfirmDialog({
    title: '删除项目',
    message: `确定要删除【${name}】吗？`,
    confirmButtonColor: 'var(--accent-red)'
  }).then(async () => {
    try {
      await deleteMenuItem(id);
      showToast('已删除');
      showMenuPopup.value = false;
      editingItem.value = null;
      await fetchAllData();
    } catch (e) {
      showToast('删除失败');
    }
  }).catch(() => {});
};

// ─── 子选项管理方法 ───────────────────────────────

const showOptionsEditor = async (item) => {
  optionEditingItem.value = item;
  optionsList.value = [];
  optionForm.value = { name: '', nameJa: '', price: '', duration: '', sortOrder: '' };
  showOptionPopup.value = true;
  try {
    const res = await getAdminMenuOptions(item.id);
    if (res.code === 200) optionsList.value = res.data || [];
  } catch (e) {
    console.error('获取子选项失败', e);
  }
};

const editOption = (opt) => {
  optionForm.value = {
    id: opt.id,
    menuItemId: optionEditingItem.value.id,
    name: opt.name || '',
    nameJa: opt.nameJa || '',
    price: String(opt.price || ''),
    duration: String(opt.duration || ''),
    sortOrder: String(opt.sortOrder || ''),
  };
};

const cancelOptionEdit = () => {
  optionForm.value = { name: '', nameJa: '', price: '', duration: '', sortOrder: '' };
};

const saveOption = async () => {
  if (!optionForm.value.name || !optionForm.value.price) return;
  const data = {
    menuItemId: optionEditingItem.value.id,
    name: optionForm.value.name,
    nameJa: optionForm.value.nameJa || null,
    price: parseInt(optionForm.value.price),
    duration: optionForm.value.duration ? parseInt(optionForm.value.duration) : 0,
    sortOrder: optionForm.value.sortOrder ? parseInt(optionForm.value.sortOrder) : 0,
    isActive: 1,
  };
  try {
    if (optionForm.value.id) {
      await updateMenuOption(optionForm.value.id, data);
      showToast('更新成功');
    } else {
      await createMenuOption(data);
      showToast('添加成功');
    }
    const res = await getAdminMenuOptions(optionEditingItem.value.id);
    if (res.code === 200) optionsList.value = res.data || [];
    optionForm.value = { name: '', nameJa: '', price: '', duration: '', sortOrder: '' };
  } catch (e) {
    showToast('操作失败');
  }
};

const handleDeleteOption = (opt) => {
  showConfirmDialog({
    title: '删除选项',
    message: `确定要删除【${opt.name}】吗？`,
    confirmButtonColor: 'var(--accent-red)'
  }).then(async () => {
    try {
      await deleteMenuOption(opt.id);
      showToast('已删除');
      const res = await getAdminMenuOptions(optionEditingItem.value.id);
      if (res.code === 200) optionsList.value = res.data || [];
    } catch (e) {
      showToast('删除失败');
    }
  }).catch(() => {});
};

onMounted(() => {
  if (sessionStorage.getItem('admin_auth') === 'true') {
    isAuthenticated.value = true;
    fetchAllData();
  }
});

// ─── 定金管理方法 ──────────────────────────────────

const depositTagType = (status) => {
  if (status === 'PAID') return 'success';
  if (status === 'CUSTOMER_PAID') return 'primary';
  if (status === 'PENDING_REFUND') return 'warning';
  if (status === 'REFUNDED') return 'default';
  if (status === 'FORFEITED') return 'danger';
  return 'default';
};

const depositStatusLabel = (r) => {
  const amount = r.depositAmount > 0 ? `¥${r.depositAmount.toLocaleString()}` : '¥1,000';
  if (r.depositStatus === 'PAID')            return `${amount} ✅ 已收款`;
  if (r.depositStatus === 'CUSTOMER_PAID')   return `${amount} 💰 客户已付款`;
  if (r.depositStatus === 'PENDING_REFUND')  return `${amount} ⚠️ 退款待处理`;
  if (r.depositStatus === 'REFUNDED')        return `${amount} ↩️ 已退款`;
  if (r.depositStatus === 'FORFEITED')       return `${amount} 🚫 已没收`;
  return `${amount} ⏳ 未收款`;
};

const handleDepositPaid = async (id) => {
  try {
    await markDepositPaid(id);
    showToast('定金已标记为已收款');
    await fetchAllData();
    showPopup.value = false;
  } catch (e) {
    showToast('操作失败');
  }
};

const handleDepositRefunded = async (id) => {
  showConfirmDialog({
    title: '退款确认',
    message: '确定要退还该预约的定金吗？',
    confirmButtonColor: 'var(--accent-green)'
  }).then(async () => {
    try {
      await markDepositRefunded(id);
      showToast('定金已退款');
      await fetchAllData();
      showPopup.value = false;
    } catch (e) {
      showToast('退款失败');
    }
  }).catch(() => {});
};

const handleForfeitDeposit = async (id) => {
  showConfirmDialog({
    title: '没收定金',
    message: '客户未到店，确定没收定金吗？',
    confirmButtonColor: 'var(--accent-red)'
  }).then(async () => {
    try {
      await forfeitDeposit(id);
      showToast('定金已没收');
      await fetchAllData();
      showPopup.value = false;
    } catch (e) {
      showToast('操作失败');
    }
  }).catch(() => {});
};

</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background-color: #f7f8fa;
}
.login-box {
  padding-top: 100px;
}
.title {
  text-align: center;
  color: #323233;
  margin-bottom: 20px;
  font-weight: 500;
}

/* 视图切换 */
.tabs-wrapper {
  background: white;
  padding: 10px 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  margin-bottom: 12px;
}

/* 周视图 */
.week-view {
  padding: 0 12px;
}
.summary-tip {
  font-size: 13px;
  color: #969799;
  text-align: center;
  margin-bottom: 12px;
}
.week-list {
  box-shadow: 0 4px 12px rgba(0,0,0,0.03);
}
.day-title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.day-date {
  font-weight: bold;
  font-size: 15px;
  color: #323233;
}
.day-week {
  font-size: 13px;
  color: #969799;
}

/* 日视图 */
.schedule-container {
  padding: 0 12px;
}
.date-picker-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: white;
  padding: 10px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
}
.current-date {
  font-size: 16px;
  font-weight: bold;
  color: #1989fa;
  display: flex;
  align-items: center;
  gap: 6px;
}
.timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.time-block-wrapper {
  display: flex;
  align-items: stretch;
  min-height: 80px;
}
.time-label {
  width: 50px;
  font-size: 14px;
  font-weight: 500;
  color: #969799;
  text-align: right;
  padding-right: 12px;
  padding-top: 10px;
  border-right: 2px solid #ebedf0;
}

/* 卡片样式 */
.slot-card {
  flex: 1;
  margin-left: 12px;
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  transition: all 0.2s;
  cursor: pointer;
}
.slot-card.available {
  border: 1.5px dashed #c8c9cc;
  color: #969799;
  align-items: center;
  font-size: 14px;
  background-color: transparent;
}
.slot-card.available:active {
  background-color: #f2f3f5;
}
.slot-card.locked {
  background-color: #f2f3f5;
  color: #646566;
  align-items: center;
  font-size: 14px;
}
.slot-card.booked {
  background-color: #e8f3fe; 
  border-left: 4px solid #1989fa;
}
.slot-card.booked.new-customer {
  background-color: #e1f3d8; 
  border-left: 4px solid #67c23a;
}
.slot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.slot-header .name {
  font-weight: bold;
  font-size: 15px;
  color: #323233;
}
.slot-summary {
  font-size: 13px;
  color: #646566;
}

/* 弹窗样式 */
.popup-content {
  padding: 16px;
}
.popup-title {
  text-align: center;
  margin-bottom: 16px;
  color: #323233;
}
.reference-image-box {
  padding: 16px;
  text-align: center;
}
.img-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 10px;
  text-align: left;
}
.ref-img {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* ─── 菜单管理样式 ─── */
.menu-mgmt-view {
  padding: 0 12px 40px;
}
.menu-mgmt-header {
  padding: 12px 0;
}
.menu-group {
  margin-bottom: 16px;
}
.menu-group-title {
  font-size: 15px;
  font-weight: 600;
  color: #323233;
  padding: 8px 4px;
}
.menu-item-cell {
  border-radius: 8px;
}
.menu-item-name {
  font-size: 15px;
  font-weight: 500;
}
.menu-item-name-ja {
  font-size: 12px;
  color: #969799;
  font-weight: 400;
}
.menu-item-meta {
  font-size: 13px;
  color: #969799;
  margin-top: 4px;
}
.item-price {
  color: var(--accent-red);
  font-weight: 600;
}
.menu-editor {
  padding: 16px;
}
.menu-editor .popup-title {
  text-align: center;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: 600;
}
</style>
