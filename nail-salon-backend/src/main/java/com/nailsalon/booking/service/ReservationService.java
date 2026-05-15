package com.nailsalon.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nailsalon.booking.dto.ReservationRequest;
import com.nailsalon.booking.entity.LockedSlot;
import com.nailsalon.booking.entity.MenuItem;
import com.nailsalon.booking.entity.MenuItemOption;
import com.nailsalon.booking.entity.Reservation;
import com.nailsalon.booking.mapper.LockedSlotMapper;
import com.nailsalon.booking.mapper.MenuItemMapper;
import com.nailsalon.booking.mapper.MenuItemOptionMapper;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private final ReservationMapper reservationMapper;
    private final LockedSlotMapper lockedSlotMapper;
    private final StoreSettingsService storeSettingsService;
    private final NotifyService notifyService;
    private final MenuItemMapper menuItemMapper;
    private final MenuItemOptionMapper menuItemOptionMapper;

    /** 最低时长：2小时（120分钟） */
    private static final int MIN_SLOT_DURATION = 120;

    /** AWAI 标准时间点（6个起始时间，90分钟间隔） */
    private static final List<String> AWAI_SLOTS = Arrays.asList(
            "11:00", "12:30", "14:00", "15:30", "17:00", "18:30"
    );

    public List<String> getAwaiSlots() {
        return AWAI_SLOTS;
    }

    /**
     * 计算有效时长（分钟）
     * max(最低2小时, 主菜单时长 + 子选项总时长)
     */
    public int calculateEffectiveDuration(Long menuItemId, List<Long> optionIds) {
        int baseDuration = 0;
        if (menuItemId != null) {
            MenuItem item = menuItemMapper.selectById(menuItemId);
            if (item != null && item.getDuration() != null) {
                baseDuration = item.getDuration();
            }
        }
        int optionDuration = 0;
        if (optionIds != null && !optionIds.isEmpty()) {
            optionDuration = menuItemOptionMapper.selectList(
                    new LambdaQueryWrapper<MenuItemOption>().in(MenuItemOption::getId, optionIds)
            ).stream().filter(o -> o.getDuration() != null).mapToInt(MenuItemOption::getDuration).sum();
        }
        return Math.max(MIN_SLOT_DURATION, baseDuration + optionDuration);
    }

    /**
     * 获取营业时间设定
     */
    private LocalTime getOpenTime() {
        String val = storeSettingsService.getAllSettings().getOrDefault("open_time", "10:00");
        return LocalTime.parse(val);
    }

    private LocalTime getCloseTime() {
        String val = storeSettingsService.getAllSettings().getOrDefault("close_time", "20:00");
        return LocalTime.parse(val);
    }

    /**
     * 获取可用时段（动态：根据菜单时长计算）
     * 无 menuItemId 时向后兼容返回 2小时固定块
     */
    public List<Map<String, Object>> getAvailability(LocalDate date, Long menuItemId, List<Long> optionIds) {
        List<String> bookedSlots = reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getReserveDate, date)
                        .ne(Reservation::getStatus, "CANCELLED")
        ).stream().map(Reservation::getTimeSlot).collect(Collectors.toList());

        List<String> lockedSlots = lockedSlotMapper.selectList(
                new LambdaQueryWrapper<LockedSlot>()
                        .eq(LockedSlot::getLockDate, date)
        ).stream().map(LockedSlot::getTimeSlot).collect(Collectors.toList());

        LocalTime now = LocalTime.now();
        boolean isToday = date.equals(LocalDate.now());

        List<Map<String, Object>> result = new ArrayList<>();

        int effectiveDuration = calculateEffectiveDuration(menuItemId, optionIds);
        LocalTime closeTime = getCloseTime();

        for (String startStr : AWAI_SLOTS) {
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = start.plusMinutes(effectiveDuration);

            // 必须能在打烊前结束
            if (end.isAfter(closeTime)) {
                result.add(Map.of("timeSlot", startStr + "-" + end.toString(), "available", false));
                continue;
            }

            String slotRange = startStr + "-" + end.toString();

            // 检查是否被预约/锁定
            boolean isBooked = bookedSlots.stream().anyMatch(s -> overlaps(s, slotRange));
            boolean isLocked = lockedSlots.stream().anyMatch(s -> overlaps(s, slotRange));
            boolean isAvailable = !isBooked && !isLocked;

            // 今天的过去时间不可选
            if (isToday && isAvailable) {
                if (now.isAfter(start)) {
                    isAvailable = false;
                }
            }

            result.add(Map.of("timeSlot", slotRange, "available", isAvailable));
        }

        return result;
    }

    /** 两个时间范围是否重叠（格式: "HH:mm-HH:mm"） */
    private boolean overlaps(String rangeA, String rangeB) {
        String[] partsA = rangeA.split("-");
        String[] partsB = rangeB.split("-");
        if (partsA.length != 2 || partsB.length != 2) return false;
        LocalTime aStart = LocalTime.parse(partsA[0]);
        LocalTime aEnd = LocalTime.parse(partsA[1]);
        LocalTime bStart = LocalTime.parse(partsB[0]);
        LocalTime bEnd = LocalTime.parse(partsB[1]);
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
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

        // 新客防刷校验
        if ("NEW".equals(req.getCustomerType())) {
            boolean historyExists = reservationMapper.countByContactIdIncludeDeleted(req.getContactId()) > 0;
            if (historyExists) {
                throw new RuntimeException("检测到该联系方式已有预约记录，请返回首页以【老客】身份预约。");
            }
        }

        // 计算实际时间段（根据菜单时长动态计算）
        String timeSlot = req.getTimeSlot();
        if (req.getMenuItemId() != null) {
            int effectiveDuration = calculateEffectiveDuration(req.getMenuItemId(), req.getOptionIds());
            String startStr = req.getTimeSlot().split("-")[0];
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = start.plusMinutes(effectiveDuration);
            timeSlot = startStr + "-" + end.toString();
        }

        // 防撞单检查
        boolean exists = reservationMapper.exists(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getReserveDate, reserveDate)
                .eq(Reservation::getTimeSlot, timeSlot)
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
        res.setTimeSlot(timeSlot);
        res.setStatus("PENDING_DEPOSIT");
        res.setCreatedAt(LocalDateTime.now());
        res.setMenuItemId(req.getMenuItemId());

        // 子选项ID存为JSON
        if (req.getOptionIds() != null && !req.getOptionIds().isEmpty()) {
            res.setSelectedOptions(req.getOptionIds().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }

        // 服务总价 & 动态定金计算
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

        reservationMapper.insert(res);

        // 发送新预约通知
        notifyService.notifyNewReservation(res);

        return res.getId();
    }

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

    public List<Reservation> getReservationsByContactId(String contactId) {
        autoCompleteExpiredReservations();
        return reservationMapper.selectList(
                new LambdaQueryWrapper<Reservation>()
                        .eq(Reservation::getContactId, contactId)
                        .orderByDesc(Reservation::getReserveDate)
                        .orderByDesc(Reservation::getCreatedAt)
        );
    }

    public boolean hasReservationHistory(String contactId) {
        return reservationMapper.countByContactIdIncludeDeleted(contactId) > 0;
    }

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
        for (String startStr : AWAI_SLOTS) {
            String slot = startStr + "-" + LocalTime.parse(startStr).plusMinutes(MIN_SLOT_DURATION).toString();
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
        Reservation reservation = reservationMapper.selectById(id);
        if (reservation == null) {
            log.warn("取消预约失败：ID={} 不存在", id);
            return;
        }
        reservationMapper.deleteById(id);
        notifyService.notifyCancelledReservation(reservation);
    }

    @Transactional
    public void restoreReservation(Long id) {
        Reservation reservation = reservationMapper.selectByIdIncludeDeleted(id);
        if (reservation == null) {
            throw new RuntimeException("未找到该预约记录");
        }
        if (reservation.getIsDeleted() != 1) {
            throw new RuntimeException("该预约未被取消，无需恢复");
        }

        int historyCount = reservationMapper.countByContactIdIncludeDeleted(reservation.getContactId());
        if (historyCount == 0) {
            throw new RuntimeException("该客户无历史预约记录，无法恢复");
        }

        int affected = reservationMapper.restoreById(id);
        if (affected != 1) {
            throw new RuntimeException("恢复预约失败");
        }

        reservation.setIsDeleted(0);
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
        List<String> allSlots = AWAI_SLOTS.stream()
                .map(s -> s + "-" + LocalTime.parse(s).plusMinutes(MIN_SLOT_DURATION).toString())
                .collect(Collectors.toList());
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

    @Transactional
    public void markDepositRefunded(Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"FORFEITED".equals(r.getDepositStatus()) && !"PAID".equals(r.getDepositStatus())) {
            throw new RuntimeException("仅可退款已收或已没收的定金");
        }
        r.setDepositStatus("REFUNDED");
        r.setDepositPaidAt(LocalDateTime.now());
        reservationMapper.updateById(r);
        notifyService.notifyDepositRefunded(r);
    }

    @Transactional
    public void forfeitDeposit(Long reservationId) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"PAID".equals(r.getDepositStatus())) {
            throw new RuntimeException("仅可没收已收款的定金");
        }
        r.setDepositStatus("FORFEITED");
        reservationMapper.updateById(r);
    }

    @Transactional
    public void simulateDepositPayment(Long reservationId, String method) {
        Reservation r = reservationMapper.selectById(reservationId);
        if (r == null) throw new RuntimeException("预约不存在");
        if (!"NONE".equals(r.getDepositStatus())) {
            throw new RuntimeException("定金已支付，请勿重复操作");
        }
        r.setDepositStatus("PAID");
        r.setDepositPaidAt(LocalDateTime.now());
        r.setStatus("CONFIRMED");
        reservationMapper.updateById(r);
    }

    @Transactional
    public void rescheduleReservation(Long id, String dateStr, String timeSlot) {
        Reservation r = reservationMapper.selectById(id);
        if (r == null) throw new RuntimeException("预约不存在");
        LocalDate newDate = LocalDate.parse(dateStr);
        LocalDate today = LocalDate.now();
        if (newDate.isBefore(today)) throw new RuntimeException("不能改期到过去的日期");

        // 检查新时间段是否被占用
        boolean exists = reservationMapper.exists(new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getReserveDate, newDate)
                .eq(Reservation::getTimeSlot, timeSlot)
                .ne(Reservation::getStatus, "CANCELLED")
                .ne(Reservation::getId, id));
        if (exists) throw new RuntimeException("该时间段已被预订");

        // 根据菜单重新计算时间段
        String finalTimeSlot = timeSlot;
        if (r.getMenuItemId() != null) {
            List<Long> optionIds = null;
            if (r.getSelectedOptions() != null && !r.getSelectedOptions().isBlank()) {
                optionIds = Arrays.stream(r.getSelectedOptions().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong)
                        .collect(Collectors.toList());
            }
            int effectiveDuration = calculateEffectiveDuration(r.getMenuItemId(), optionIds);
            String startStr = timeSlot.split("-")[0];
            LocalTime start = LocalTime.parse(startStr);
            LocalTime end = start.plusMinutes(effectiveDuration);
            finalTimeSlot = startStr + "-" + end.toString();
        }

        r.setReserveDate(newDate);
        r.setTimeSlot(finalTimeSlot);
        r.setStatus("PENDING_DEPOSIT");
        reservationMapper.updateById(r);
        notifyService.notifyRescheduledReservation(r);
    }

    // ─── 计算总价 ──────────────────────────

    public int calculateTotalAmount(Long menuItemId, List<Long> optionIds, boolean isNewCustomer) {
        int total = 0;
        if (menuItemId != null) {
            MenuItem item = menuItemMapper.selectById(menuItemId);
            if (item != null && item.getPrice() != null) {
                total += item.getPrice();
            }
        }
        if (optionIds != null && !optionIds.isEmpty()) {
            total += menuItemOptionMapper.selectList(
                    new LambdaQueryWrapper<MenuItemOption>().in(MenuItemOption::getId, optionIds)
            ).stream().filter(o -> o.getPrice() != null).mapToInt(MenuItemOption::getPrice).sum();
        }
        if (isNewCustomer) {
            total = (int) Math.round(total * 0.8);
        }
        return total;
    }
}
