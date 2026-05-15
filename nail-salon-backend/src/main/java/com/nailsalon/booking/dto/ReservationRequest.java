package com.nailsalon.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReservationRequest {
    @NotBlank(message = "客户类型不能为空")
    private String customerType;
    @NotBlank(message = "姓名不能为空")
    private String name;
    @NotBlank(message = "联系方式不能为空")
    private String contactId;
    @NotBlank(message = "必须选择卸甲类型")
    private String removalType;
    private String remarks;
    private String referenceImage;
    private Integer totalAmount;
    @NotNull(message = "预约日期不能为空")
    private LocalDate reserveDate;
    @NotBlank(message = "预约时间段不能为空")
    private String timeSlot;
    private Long menuItemId;
    private List<Long> optionIds;
    private Integer totalDuration;
}
