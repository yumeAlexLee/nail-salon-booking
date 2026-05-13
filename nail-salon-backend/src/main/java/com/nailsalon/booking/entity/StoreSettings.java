package com.nailsalon.booking.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("store_settings")
public class StoreSettings {
    @TableId
    private String settingKey;
    private String settingValue;
}
