package com.nailsalon.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reservation")
public class Reservation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerType;
    private String name;
    private String contactId;
    private String removalType;
    private String remarks;
    private String referenceImage;
    private LocalDate reserveDate;
    private String timeSlot;
    private String status;
    private LocalDateTime createdAt;
    
    @com.baomidou.mybatisplus.annotation.TableLogic
    private Integer isDeleted;

    // ─── 定金相关字段 ───────────────────────────────
    private Integer depositAmount;
    private String depositStatus;
    private LocalDateTime depositPaidAt;
}
