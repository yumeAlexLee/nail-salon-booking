package com.nailsalon.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class LockRequest {
    @NotNull(message = "日期不能为空")
    private LocalDate lockDate;

    @NotBlank(message = "时间段不能为空")
    private String timeSlot;
}
