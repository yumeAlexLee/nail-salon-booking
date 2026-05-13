import axios from 'axios';

const api = axios.create({
  baseURL: '/api',
  timeout: 5000,
});

const ADMIN_TOKEN = 'nail-salon-admin-2026';

const adminApi = axios.create({
  baseURL: '/api',
  timeout: 5000,
  headers: { 'Authorization': `Bearer ${ADMIN_TOKEN}` },
});

// 获取可用时间
export const getAvailability = (date) => {
  return api.get('/availability', { params: { date } }).then(res => res.data);
};

// 提交预约
export const submitReservation = (data) => {
  return api.post('/reserve', data).then(res => res.data);
};

// 后台：获取所有预约记录
export const getAdminReservations = () => {
  return adminApi.get('/reservations/admin/list').then(res => res.data);
};

// 后台：获取所有已取消的预约记录
export const getCancelledReservations = () => {
  return adminApi.get('/reservations/admin/cancelled').then(res => res.data);
};

// 后台：获取所有被锁定的时间段
export const getAdminLockedSlots = () => {
  return adminApi.get('/reservations/admin/locked').then(res => res.data);
};

// 后台：锁定时间段
export const lockSlot = (lockDate, timeSlot) => {
  return adminApi.post('/reservations/admin/lock', { lockDate, timeSlot }).then(res => res.data);
};

// 后台：解锁时间段
export const unlockSlot = (lockDate, timeSlot) => {
  return adminApi.post('/reservations/admin/unlock', { lockDate, timeSlot }).then(res => res.data);
};

// 后台：锁定全天
export const lockDay = (date) => {
  return adminApi.post(`/reservations/admin/lock-day?date=${date}`).then(res => res.data);
};

// 后台：解锁全天
export const unlockDay = (date) => {
  return adminApi.post(`/reservations/admin/unlock-day?date=${date}`).then(res => res.data);
};

// 后台：取消/删除预约
export const deleteReservation = (id) => {
  return adminApi.delete(`/reservations/admin/${id}`).then(res => res.data);
};

// 后台：恢复已取消的预约
export const restoreReservation = (id) => {
  return adminApi.post(`/reservations/admin/${id}/restore`).then(res => res.data);
};

// ─── 定金管理 ─────────────────────────────────────

// 获取默认定金金额
export const getDefaultDeposit = () => {
  return adminApi.get('/deposit/default').then(res => res.data);
};

// 更新默认定金金额
export const updateDefaultDeposit = (amount) => {
  return adminApi.post('/deposit/default', { amount }).then(res => res.data);
};

// 标记定金已收款
export const markDepositPaid = (id) => {
  return adminApi.post(`/deposit/${id}/paid`).then(res => res.data);
};

// 标记定金已退款
export const markDepositRefunded = (id) => {
  return adminApi.post(`/deposit/${id}/refunded`).then(res => res.data);
};

// 没收定金
export const forfeitDeposit = (id) => {
  return adminApi.post(`/deposit/${id}/forfeit`).then(res => res.data);
};

// ─── 模拟支付（客户预付定金） ─────────────────────────
// 客户在预约成功页点击支付按钮后调用
// method: 'paypay' | 'wechat' | 'alipay' | 'simulate'

export const simulatePay = (id, method = 'simulate') => {
  return adminApi.post(`/deposit/${id}/simulate-pay`, { method }).then(res => res.data);
};

// 客户声称已付款，等待店主确认
export const claimPaid = (id) => {
  return adminApi.post(`/deposit/${id}/claim-paid`).then(res => res.data);
};

// 后台：获取店铺设置
export const getSettings = () => {
  return adminApi.get('/settings').then(res => res.data);
};

// 后台：更新店铺设置
export const updateSettings = (data) => {
  return adminApi.post('/settings', data).then(res => res.data);
};

// 上传图片
export const uploadImage = (formData) => {
  return adminApi.post('/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data', 'Authorization': `Bearer ${ADMIN_TOKEN}` }
  }).then(res => res.data);
};

// ─── 用户端：预约历史 + 取消预约 ─────────────────

// 按联系方式查询预约记录
export const getReservationsByContact = (contactId) => {
  return api.get('/reservations', { params: { contactId } }).then(res => res.data);
};

// 用户自行取消预约
export const userCancelReservation = (id) => {
  return api.post(`/reservations/${id}/cancel`).then(res => res.data);
};

// 用户改期预约
export const rescheduleReservation = (id, date, timeSlot) =>
  api.post(`/reservations/${id}/reschedule`, { date, timeSlot }).then(res => res.data);

// ─── 用户身份识别 ──────────────────────────────

// 查询联系方式是否有历史记录（自动判断新客/老客）
export const getCustomerStatus = (contactId) => {
  return api.get('/customer/status', { params: { contactId } }).then(res => res.data);
};

// ─── 菜单管理 ─────────────────────────────────────

// 获取启用的菜单（按分类分组）
export const getActiveMenu = () => {
  return api.get('/menu/active').then(res => res.data);
};

// 获取所有菜单项目
export const getAllMenuItems = () => {
  return adminApi.get('/menu/admin/list').then(res => res.data);
};

// 新增菜单项目
export const createMenuItem = (data) => {
  return adminApi.post('/menu/admin', data).then(res => res.data);
};

// 更新菜单项目
export const updateMenuItem = (id, data) => {
  return adminApi.put(`/menu/admin/${id}`, data).then(res => res.data);
};

// 删除菜单项目
export const deleteMenuItem = (id) => {
  return adminApi.delete(`/menu/admin/${id}`).then(res => res.data);
};
