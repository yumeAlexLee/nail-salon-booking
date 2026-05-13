package com.nailsalon.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("portfolio_image")
public class PortfolioImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String imageUrl;
    private String tag;
    private String category;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime createdAt;
}
