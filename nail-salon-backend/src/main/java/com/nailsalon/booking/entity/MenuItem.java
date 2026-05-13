package com.nailsalon.booking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("menu_item")
public class MenuItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String category;
    private String categoryJa;
    private String name;
    private String nameJa;
    private Integer price;
    private Integer duration;
    private String description;
    private String descriptionJa;
    private Integer sortOrder;
    
    // ─── 示意图（后续上传，URL 指向 /uploads/menu/xxx.jpg） ───
    private String imageUrl;
    
    private Integer isActive;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
