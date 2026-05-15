package com.nailsalon.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("menu_option")
public class MenuItemOption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long menuItemId;
    private String name;
    private String nameJa;
    private Integer price;
    private Integer duration;
    private Integer sortOrder;
    private Integer isActive;
    private LocalDateTime createdAt;
}
