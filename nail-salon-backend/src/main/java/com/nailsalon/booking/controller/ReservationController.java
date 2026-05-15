package com.nailsalon.booking.controller;

import com.nailsalon.booking.dto.ApiResponse;
import com.nailsalon.booking.dto.ReservationRequest;
import com.nailsalon.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/availability")
    public ApiResponse<List<Map<String, Object>>> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long menuItemId,
            @RequestParam(required = false) String optionIds) {
        List<Long> ids = null;
        if (optionIds != null && !optionIds.isBlank()) {
            ids = Arrays.stream(optionIds.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        return ApiResponse.success(reservationService.getAvailability(date, menuItemId, ids));
    }

    @GetMapping("/availability/estimate")
    public ApiResponse<Map<String, Object>> estimateAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long menuItemId,
            @RequestParam(required = false) String optionIds) {
        List<Long> ids = null;
        if (optionIds != null && !optionIds.isBlank()) {
            ids = Arrays.stream(optionIds.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        int duration = reservationService.calculateEffectiveDuration(menuItemId, ids);
        int totalPrice = reservationService.calculateTotalAmount(menuItemId, ids, false);
        return ApiResponse.success(Map.of(
                "totalDuration", duration,
                "totalPrice", totalPrice
        ));
    }

    @PostMapping("/reserve")
    public ApiResponse<Long> reserve(@Validated @RequestBody ReservationRequest request) {
        Long id = reservationService.submitReservation(request);
        return ApiResponse.success(id);
    }

    // ================= 用户端 API =================

    @GetMapping("/reservations")
    public ApiResponse<List<com.nailsalon.booking.entity.Reservation>> getReservations(
            @RequestParam String contactId) {
        return ApiResponse.success(reservationService.getReservationsByContactId(contactId));
    }

    @PostMapping("/reservations/{id}/cancel")
    public ApiResponse<Void> userCancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        reservationService.userCancelReservation(id, reason);
        return ApiResponse.success(null);
    }

    @PostMapping("/reservations/{id}/reschedule")
    public ApiResponse<Void> reschedule(@PathVariable Long id, @RequestBody Map<String, String> body) {
        reservationService.rescheduleReservation(id, body.get("date"), body.get("timeSlot"));
        return ApiResponse.success(null);
    }

    // ================= 用户身份识别 =================

    @GetMapping("/customer/status")
    public ApiResponse<Map<String, Object>> getCustomerStatus(@RequestParam String contactId) {
        boolean hasHistory = reservationService.hasReservationHistory(contactId);
        return ApiResponse.success(Map.of(
                "isNew", !hasHistory,
                "customerType", hasHistory ? "OLD" : "NEW"
        ));
    }

    // ================= 管理端 API =================

    @GetMapping("/reservations/admin/list")
    public ApiResponse<List<com.nailsalon.booking.entity.Reservation>> getAdminReservations() {
        return ApiResponse.success(reservationService.getAllReservations());
    }

    @GetMapping("/reservations/admin/cancelled")
    public ApiResponse<List<com.nailsalon.booking.entity.Reservation>> getCancelledReservations() {
        return ApiResponse.success(reservationService.getCancelledReservations());
    }

    @GetMapping("/reservations/admin/locked")
    public ApiResponse<List<com.nailsalon.booking.entity.LockedSlot>> getAdminLockedSlots() {
        return ApiResponse.success(reservationService.getAllLockedSlots());
    }

    @PostMapping("/reservations/admin/lock")
    public ApiResponse<Void> lockSlot(@Validated @RequestBody com.nailsalon.booking.dto.LockRequest request) {
        reservationService.lockSlot(request.getLockDate(), request.getTimeSlot());
        return ApiResponse.success(null);
    }

    @PostMapping("/reservations/admin/unlock")
    public ApiResponse<Void> unlockSlot(@Validated @RequestBody com.nailsalon.booking.dto.LockRequest request) {
        reservationService.unlockSlot(request.getLockDate(), request.getTimeSlot());
        return ApiResponse.success(null);
    }

    @PostMapping("/reservations/admin/lock-day")
    public ApiResponse<Void> lockDay(@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        reservationService.lockDay(date);
        return ApiResponse.success(null);
    }

    @PostMapping("/reservations/admin/unlock-day")
    public ApiResponse<Void> unlockDay(@RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        reservationService.unlockDay(date);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/reservations/admin/{id}")
    public ApiResponse<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/reservations/admin/{id}/restore")
    public ApiResponse<Void> restoreReservation(@PathVariable Long id) {
        reservationService.restoreReservation(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/reservations/admin/{id}/cancel")
    public ApiResponse<Void> adminCancel(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        reservationService.adminCancelReservation(id, reason);
        return ApiResponse.success(null);
    }

    @GetMapping("/admin/week-summary")
    public ApiResponse<Map<String, Object>> getWeekSummary() {
        return ApiResponse.success(reservationService.getWeekSummary());
    }

    // ================= 定金管理 =================

    @GetMapping("/deposit/default")
    public ApiResponse<Integer> getDefaultDeposit() {
        return ApiResponse.success(reservationService.getDefaultDeposit());
    }

    @PostMapping("/deposit/default")
    public ApiResponse<Void> updateDefaultDeposit(@RequestBody java.util.Map<String, Integer> body) {
        reservationService.updateDefaultDeposit(body.getOrDefault("amount", 1000));
        return ApiResponse.success(null);
    }

    @PostMapping("/deposit/{id}/paid")
    public ApiResponse<Void> markPaid(@PathVariable Long id) {
        reservationService.markDepositPaid(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/deposit/{id}/refunded")
    public ApiResponse<Void> markRefunded(@PathVariable Long id) {
        reservationService.markDepositRefunded(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/deposit/{id}/forfeit")
    public ApiResponse<Void> forfeit(@PathVariable Long id) {
        reservationService.forfeitDeposit(id);
        return ApiResponse.success(null);
    }

    // ─── 模拟支付（客户预付定金） ────────────────────────
    // 客户在预约成功页点击「模拟支付」后调用此接口
    // 将定金状态从 NONE 标记为 PAID，记录支付时间和方式
    // 后续可替换为真实支付网关（PayPay / Stripe 等）

    @PostMapping("/deposit/{id}/claim-paid")
    public ApiResponse<Void> claimPaid(@PathVariable Long id) {
        reservationService.customerClaimPaid(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/deposit/{id}/simulate-pay")
    public ApiResponse<Void> simulatePay(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String method = (body != null) ? body.getOrDefault("method", "simulate") : "simulate";
        reservationService.simulateDepositPayment(id, method);
        return ApiResponse.success(null);
    }
}
