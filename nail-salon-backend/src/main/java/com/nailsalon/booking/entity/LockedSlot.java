package com.nailsalon.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("locked_slot")
public class LockedSlot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private LocalDate lockDate;
    private String timeSlot;
    private String reason;
    private LocalDateTime createdAt;
}
