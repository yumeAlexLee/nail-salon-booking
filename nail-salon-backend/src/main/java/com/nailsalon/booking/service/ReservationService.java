package com.nailsalon.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nailsalon.booking.dto.ReservationRequest;
import com.nailsalon.booking.entity.LockedSlot;
import com.nailsalon.booking.entity.Reservation;
import com.nailsalon.booking.mapper.LockedSlotMapper;
import com.nailsalon.booking.mapper.ReservationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private final ReservationMapper reservationMapper;
    private final LockedSlotMapper lockedSlotMapper;
    private final StoreSettingsService storeSettingsService;
    private final NotifyService notifyService;

    private static final List<String> AWAI_SLOTS = Arrays.asList(
            "11:00", "12:30", "14:00", "15:30", "17:00", "18:30"
    );

    /** AWAI 标准时间点（6个槽，90分钟间隔） */
    public List<String> getAwaiSlots() {
        return AWAI_SLOTS;
    }

    public List<String> getAllSlots() {
        Map<String, String> settings = storeSettingsService.getAllSettings();
        String openTimeStr = settings.getOrDefault("open_time", "10:00");
        String closeTimeStr = settings.getOrDefault("close_time", "20:00");

        LocalTime openTime = LocalTime.parse(openTimeStr);
        LocalTime closeTime = LocalTime.parse(closeTimeStr);

        List<String> slots = new ArrayList<>();
        LocalTime current = openTime;
        while (current.plusHours(2).isBefore(closeTime) || current.plusHours(2).equals(closeTime)) {
            slots.add(current.toString() + "-" + current.plusHours(2).toString());
            current = current.plusHours(2);
        }
        return slots;
    }

    public List<Map<String, Object>> getAvailability(LocalDate date) {
        // 获取当天已被预约的时间段（排除已取消的记录）
        List<String> bookedSlots = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getReserveDate, date)
                        .ne(Reservation::getStatus, "CANCELLED")
        ).stream().map(Reservation::getTimeSlot).collect(Collectors.toList());

        // 获取当天被老板锁定的时间段
        List<String> lockedSlots = lockedSlotMapper.selectList(
                new LambdaQueryWrapper<LockedSlot>()
                        .eq(LockedSlot::getLockDate, date)
        ).stream().map(LockedSlot::getTimeSlot).collect(Collectors.toList());

        LocalTime now = LocalTime.now();
        boolean isToday = date.equals(LocalDate.now());

        List<Map<String, Object>> result = new ArrayList<>();
        List<String> allSlots = getAllSlots();
        for (String slot : allSlots) {
            boolean isAvailable = !bookedSlots.contains(slot) && !lockedSlots.contains(slot);
            
            // 如果是今天，且该时间段已经开始或过去，则置为不可用
            if (isToday && isAvailable) {
                LocalTime startTime = LocalTime.parse(slot.split("-")[0]);
                if (now.isAfter(startTime)) {
                    isAvailable = false;
                }
            }

            result.add(Map.of("timeSlot", slot, "available", isAvailable));
        }
        return result;
    }

    @Transactional
    public Long submitReservation(ReservationRequest req) {
        LocalDate reserveDate = req.getReserveDate();
        LocalDate today = LocalDate.now();
        
        if (reserveDate.isBefore(today)) {
            throw new RuntimeException("不能预约过去的日期");
        }
        if (reserveDate.isAfter(today.plusDays(14))) {
            throw new RuntimeException("最多只能提前14天预约");
        }
        if (reserveDate.equals(today)) {
            LocalTime startTime = LocalTime.parse(req.getTimeSlot().split("-")[0]);
            if (LocalTime.now().isAfter(startTime)) {
                throw new RuntimeException("该时间段已过期");
            }
        }

        // 防恶意预约：同一联系方式只能有 1 个有效的（未取消、未完成的）预约
        long activeCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getContactId, req.getContactId())
                        .ne(Reservation::getStatus, "CANCELLED")
                        .ne(Reservation::getStatus, "COMPLETED")
        );
        if (activeCount > 0) {
            throw new RuntimeException("您已有预约在进行中，如需预约新时间段请先取消当前预约。");
        }

        // 新客防刷校验：如果选了新客但联系方式在历史记录（含已取消）中存在，判定为老客
        if ("NEW".equals(req.getCustomerType())) {
            boolean historyExists = reservationMapper.countByContactIdIncludeDeleted(req.getContactId()) > 0;
            if (historyExists) {
                throw new RuntimeException("检测到该联系方式已有预约记录，请返回首页以【老客】身份预约。");
            }
        }

        // 应用层防撞单检查（排除已取消的记录）
        boolean exists = reservationMapper.exists(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getReserveDate, reserveDate)
                .eq(Reservation::getTimeSlot, req.getTimeSlot())
                .ne(Reservation::getStatus, "CANCELLED"));
        if (exists) {
            throw new RuntimeException("手慢了，该时间段已被预订");
        }

        Reservation res = new Reservation();
        res.setCustomerType(req.getCustomerType());
        res.setName(req.getName());
        res.setContactId(req.getContactId());
        res.setRemovalType(req.getRemovalType());
        res.setRemarks(req.getRemarks());
        res.setReferenceImage(req.getReferenceImage());
        res.setReserveDate(req.getReserveDate());
        res.setTimeSlot(req.getTimeSlot());
        res.setStatus("PENDING_DEPOSIT");
        res.setCreatedAt(LocalDateTime.now());

        // ─── 服务总价 & 动态定金计算 ──────────────────────
        Integer totalAmount = req.getTotalAmount();
        if (totalAmount != null && totalAmount > 0) {
            res.setTotalAmount(totalAmount);
            int percentage = getDepositPercentage();
            int deposit = (int) Math.ceil(totalAmount * percentage / 100.0);
            res.setDepositAmount(deposit);
        } else {
            res.setDepositAmount(getDefaultDeposit());
        }
        res.setDepositStatus("NONE");
        // ─────────────────────────────────────────────────

        // 插入数据库防并发：依赖数据库 (reserve_date, time_slot) 的 UNIQUE 约束。
        // 并发时只有一个能插入成功，其他的抛出 DuplicateKeyException，由全局异常捕获。
        reservationMapper.insert(res);

        // 发送新预约 Telegram 通知
        notifyService.notifyNewReservation(res);

        // 返回生成的预约 ID（供前端支付模块使用）
        return res.getId();
    }

    /**
     * 将所有预约时间已过的 PENDING_DEPOSIT/CONFIRMED 记录自动标记为 COMPLETED。
     * 在查询接口入口处调用，避免引入 @Scheduled。
     */
    @Transactional
    public void autoCompleteExpiredReservations() {
        List<Reservation> active = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .in(Reservation::getStatus, Arrays.asList("PENDING_DEPOSIT", "CONFIRMED"))
        );
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        for (Reservation r : active) {
            boolean shouldComplete = false;
            if (r.getReserveDate().isBefore(today)) {
                shouldComplete = true;
            } else if (r.getReserveDate().equals(today)) {
                String[] parts = r.getTimeSlot().split("-");
                if (parts.length == 2) {
                    LocalTime endTime = LocalTime.parse(parts[1]);
                    if (now.isAfter(endTime)) {
                        shouldComplete = true;
                    }
                }
            }
            if (shouldComplete) {
                r.setStatus("COMPLETED");
                reservationMapper.updateById(r);
            }
        }
    }

    /**
     * 按联系方式查询预约记录（用户端 - 预约历史）
     * 按预约日期倒序排列
     */
    public List<Reservation> getReservationsByContactId(String contactId) {
        autoCompleteExpiredReservations();
        return reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getContactId, contactId)
                        .orderByDesc(Reservation::getReserveDate)
                        .orderByDesc(Reservation::getCreatedAt)
        );
    }

    /**
     * 查询该联系方式是否有历史预约记录（含已取消）
     * 用于首页自动判断新客/老客
     */
    public boolean hasReservationHistory(String contactId) {
        return reservationMapper.countByContactIdIncludeDeleted(contactId) > 0;
    }

    /**
     * 用户端取消预约
     * - 改变 status 为 CANCELLED（保留记录，不是软删除）
     * - 如果定金已付 → depositStatus = PENDING_REFUND
     * - 通知店主（含退款提醒）
     */
    @Transactional
    public void userCancelReservation(Long id, String reason) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) {
            throw new RuntimeException("预约不存在");
        }
        if (!"PENDING_DEPOSIT".equals(r.getStatus()) && !"CONFIRMED".equals(r.getStatus())) {
            throw new RuntimeException("该预约状态不允许取消");
        }

        if ("PAID".equals(r.getDepositStatus()) || "CUSTOMER_PAID".equals(r.getDepositStatus())) {
            r.setDepositStatus("FORFEITED");
        }

        r.setStatus("CANCELLED");
        if (reason != null && !reason.isBlank()) {
            r.setCancelReason(reason);
        }

        reservationMapper.updateById(r);
        notifyService.notifyUserCancelledReservation(r);
    }

    @Transactional
    public void adminCancelReservation(Long id, String reason) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) throw new RuntimeException("预约不存在");
        if ("CANCELLED".equals(r.getStatus()) || "COMPLETED".equals(r.getStatus())) {
            throw new RuntimeException("该预约状态不允许取消");
        }
        r.setStatus("CANCELLED");
        if (reason != null && !reason.isBlank()) {
            r.setCancelReason(reason);
        }
        reservationMapper.updateById(r);
        notifyService.notifyCancelledReservation(r);
    }

    public List<Reservation> getAllReservations() {
        autoCompleteExpiredReservations();
        return reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .orderByDesc(Reservation::getReserveDate)
                        .orderByAsc(Reservation::getTimeSlot)
        );
    }

    /**
     * 查询所有已被取消的预约（软删除的记录）
     * 通过在 Mapper 中自定义 SQL 绕过 @TableLogic 过滤
     */
    public List<Reservation> getCancelledReservations() {
        return reservationMapper.selectCancelledList();
    }

    public List<LockedSlot> getAllLockedSlots() {
        return lockedSlotMapper.selectList(
                new LambdaQueryWrapper<LockedSlot>()
                        .orderByDesc(LockedSlot::getLockDate)
                        .orderByAsc(LockedSlot::getTimeSlot)
        );
    }

    @Transactional
    public void lockSlot(LocalDate date, String timeSlot) {
        // 先检查是否已经有人预约或已经锁定，避免老板自己撞单（排除已取消/已完成的记录）
        boolean hasReservation = reservationMapper.exists(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getReserveDate, date)
                .eq(Reservation::getTimeSlot, timeSlot)
                .ne(Reservation::getStatus, "CANCELLED")
                .ne(Reservation::getStatus, "COMPLETED"));
        if (hasReservation) throw new RuntimeException("该时间段已被客人预约，无法锁定");
        
        boolean isLocked = lockedSlotMapper.exists(new LambdaQueryWrapper<LockedSlot>()
                .eq(LockedSlot::getLockDate, date).eq(LockedSlot::getTimeSlot, timeSlot));
        if (isLocked) return;

        LockedSlot locked = new LockedSlot();
        locked.setLockDate(date);
        locked.setTimeSlot(timeSlot);
        locked.setReason("店主手动锁定");
        locked.setCreatedAt(LocalDateTime.now());
        lockedSlotMapper.insert(locked);
    }

    @Transactional
    public void unlockSlot(LocalDate date, String timeSlot) {
        lockedSlotMapper.delete(new LambdaQueryWrapper<LockedSlot>()
                .eq(LockedSlot::getLockDate, date).eq(LockedSlot::getTimeSlot, timeSlot));
    }

    @Transactional
    public void lockDay(LocalDate date) {
        List<String> allSlots = getAllSlots();
        for (String slot : allSlots) {
            boolean isLocked = lockedSlotMapper.exists(new LambdaQueryWrapper<LockedSlot>()
                    .eq(LockedSlot::getLockDate, date).eq(LockedSlot::getTimeSlot, slot));
            if (!isLocked) {
                LockedSlot locked = new LockedSlot();
                locked.setLockDate(date);
                locked.setTimeSlot(slot);
                locked.setReason("店主全天锁定");
                locked.setCreatedAt(LocalDateTime.now());
                lockedSlotMapper.insert(locked);
            }
        }
    }

    @Transactional
    public void unlockDay(LocalDate date) {
        lockedSlotMapper.delete(new LambdaQueryWrapper<LockedSlot>()
                .eq(LockedSlot::getLockDate, date));
    }

    @Transactional
    public void deleteReservation(Long id) {
        // 先查出预约详情（删了就没数据了）
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            log.warn("取消预约失败：ID={} 不存在", id);
            return;
        }

        // 执行逻辑删除（@TableLogic 自动转成 UPDATE is_deleted=1）
        reservationMapper.deleteById(id);

        // 发送取消通知给店主
        notifyService.notifyCancelledReservation(reservation);
    }

    /**
     * 恢复已取消的预约（仅限有历史记录的老客）
     *
     * 📖 知识点：恢复已逻辑删除的记录的范式
     * 1. 用自定义 SQL 绕过 @TableLogic 查到被删的记录
     * 2. 校验客户身份（老客验证）
     * 3. 用 UPDATE 直接设置 is_deleted=0
     * 4. 发送通知
     */
    @Transactional
    public void restoreReservation(Long id) {
        // 1. 查到软删除的记录（普通 selectById 查不到，因为有 @TableLogic 过滤）
        Reservation reservation = reservationMapper.selectByIdIncludeDeleted(id);
        if (reservation == null) {
            throw new RuntimeException("未找到该预约记录");
        }
        if (reservation.getIsDeleted() != 1) {
            throw new RuntimeException("该预约未被取消，无需恢复");
        }

        // 2. 老客校验：检查该客户是否曾经预约过（包括已删除的记录）
        int historyCount = reservationMapper.countByContactIdIncludeDeleted(reservation.getContactId());
        if (historyCount == 0) {
            throw new RuntimeException("该客户无历史预约记录，无法恢复");
        }

        // 3. 执行恢复
        int affected = reservationMapper.restoreById(id);
        if (affected != 1) {
            throw new RuntimeException("恢复预约失败");
        }

        // 4. 发送恢复通知
        reservation.setIsDeleted(0); // 更新内存中的状态，用于通知消息
        notifyService.notifyRestoredReservation(reservation);
    }

    // ================= 定金管理 =================

    public int getDefaultDeposit() {
        String val = storeSettingsService.getAllSettings().getOrDefault("deposit_amount", "1000");
        return Integer.parseInt(val);
    }

    public int getDepositPercentage() {
        String val = storeSettingsService.getAllSettings().getOrDefault("deposit_percentage", "30");
        return Integer.parseInt(val);
    }

    public void updateDefaultDeposit(int amount) {
        storeSettingsService.updateSetting("deposit_amount", String.valueOf(amount));
    }

    public Map<String, Object> getWeekSummary() {
        LocalDate today = LocalDate.now();
        List<String> allSlots = getAllSlots();
        int totalSlots = allSlots.size();

        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);

            List<String> booked = reservationMapper.selectList(
                    new LambdaQueryWrapper<Reservation>()
                            .eq(Reservation::getReserveDate, date)
                            .ne(Reservation::getStatus, "CANCELLED")
                            .ne(Reservation::getStatus, "COMPLETED")
            ).stream().map(Reservation::getTimeSlot).collect(Collectors.toList());

            List<String> locked = lockedSlotMapper.selectList(
                    new LambdaQueryWrapper<LockedSlot>().eq(LockedSlot::getLockDate, date)
            ).stream().map(LockedSlot::getTimeSlot).collect(Collectors.toList());

            long bookedCount = booked.size();
            long lockedCount = locked.stream().filter(s -> !booked.contains(s)).count();
            long availableCount = allSlots.stream()
                    .filter(s -> !booked.contains(s) && !locked.contains(s)).count();

            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString());
            day.put("weekday", date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.CHINESE));
            day.put("totalSlots", totalSlots);
            day.put("bookedCount", bookedCount);
            day.put("lockedCount", lockedCount);
            day.put("availableCount", availableCount);
            days.add(day);
        }

        long pendingCount = reservationMapper.selectCount(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getStatus, "PENDING_DEPOSIT")
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("pendingReservations", pendingCount);
        return result;
    }

    /**
     * 客户自报已付款，等待店主确认
     */
    @Transactional
    public void customerClaimPaid(Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"NONE".equals(r.getDepositStatus())) {
            throw new RuntimeException("定金状态异常，无需重复操作");
        }
        if (!"PENDING_DEPOSIT".equals(r.getStatus())) {
            throw new RuntimeException("该预约状态不允许操作");
        }
        r.setDepositStatus("CUSTOMER_PAID");
        r.setDepositPaidAt(LocalDateTime.now());
        reservationMapper.updateById(r);
        notifyService.notifyCustomerClaimedPaid(r);
    }

    /**
     * 标记定金已收款
     */
    @Transactional
    public void markDepositPaid(Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"NONE".equals(r.getDepositStatus()) && !"CUSTOMER_PAID".equals(r.getDepositStatus())) {
            throw new RuntimeException("定金状态不是待收款，无法操作");
        }
        r.setDepositStatus("PAID");
        r.setDepositPaidAt(LocalDateTime.now());
        r.setStatus("CONFIRMED");
        reservationMapper.updateById(r);
    }

    /**
     * 标记定金已退款
     */
    @Transactional
    public void markDepositRefunded(Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"PAID".equals(r.getDepositStatus()) && !"PENDING_REFUND".equals(r.getDepositStatus())) {
            throw new RuntimeException("定金状态不是已收款或待退款，无法退款");
        }
        r.setDepositStatus("REFUNDED");
        reservationMapper.updateById(r);
    }

    /**
     * 标记定金已没收（客户 no-show 不退定金）
     */
    @Transactional
    public void forfeitDeposit(Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"PAID".equals(r.getDepositStatus()) && !"PENDING_REFUND".equals(r.getDepositStatus())) {
            throw new RuntimeException("定金状态不是已收款或待退款，无法没收");
        }
        r.setDepositStatus("FORFEITED");
        reservationMapper.updateById(r);
    }

    // ─── 模拟支付（客户预付定金） ────────────────────────
    // 客户在预约成功页点击支付按钮后调用，将定金标记为已付款
    // 记录支付方式（paypay / wechat / alipay / simulate）和支付时间
    // 后续可替换为真实支付网关：收到回调后调用此方法或直接更新DB

    /**
     * 用户改期预约
     * - 仅允许 CONFIRMED 状态的预约改期
     * - 检查新时间段可用后更新，原时间段自动释放
     */
    @Transactional
    public void rescheduleReservation(Long id, String newDate, String newTimeSlot) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"CONFIRMED".equals(r.getStatus())) {
            throw new RuntimeException("只有已确认的预约才能改期，请先完成定金支付");
        }

        LocalDate newReserveDate = LocalDate.parse(newDate);
        LocalDate today = LocalDate.now();
        if (newReserveDate.isBefore(today)) throw new RuntimeException("不能改期到过去的日期");
        if (newReserveDate.isAfter(today.plusDays(14))) throw new RuntimeException("最多只能提前14天预约");

        // 检查新时间段是否与自己的当前时间段相同（无需改动）
        if (newReserveDate.equals(r.getReserveDate()) && newTimeSlot.equals(r.getTimeSlot())) {
            return;
        }

        // 检查新时间段是否已被占用（排除自己和已取消的记录）
        boolean taken = reservationMapper.exists(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getReserveDate, newReserveDate)
                .eq(Reservation::getTimeSlot, newTimeSlot)
                .ne(Reservation::getStatus, "CANCELLED")
                .ne(Reservation::getId, id));
        if (taken) throw new RuntimeException("该时间段已被占用，请选择其他时间");

        r.setReserveDate(newReserveDate);
        r.setTimeSlot(newTimeSlot);
        reservationMapper.updateById(r);

        notifyService.notifyRescheduledReservation(r);
    }

    @Transactional
    public void simulateDepositPayment(Long reservationId, String method) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"NONE".equals(r.getDepositStatus())) {
            throw new RuntimeException("定金已支付或已处理，无需重复操作");
        }
        r.setDepositStatus("PAID");
        r.setDepositPaidAt(LocalDateTime.now());
        r.setStatus("CONFIRMED");
        reservationMapper.updateById(r);
        log.info("💳 模拟支付成功 — 预约ID: {}, 支付方式: {}, 金额: ¥{}",
                reservationId, method, r.getDepositAmount());
    }
}
