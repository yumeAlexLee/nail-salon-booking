package com.nailsalon.booking.service;

import com.nailsalon.booking.entity.StoreSettings;
import com.nailsalon.booking.mapper.StoreSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StoreSettingsService {
    private final StoreSettingsMapper mapper;

    public Map<String, String> getAllSettings() {
        List<StoreSettings> list = mapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        for (StoreSettings s : list) {
            map.put(s.getSettingKey(), s.getSettingValue());
        }
        return map;
    }

    public void updateSetting(String key, String value) {
        StoreSettings setting = mapper.selectById(key);
        if (setting == null) {
            setting = new StoreSettings();
            setting.setSettingKey(key);
            setting.setSettingValue(value);
            mapper.insert(setting);
        } else {
            setting.setSettingValue(value);
            mapper.updateById(setting);
        }
    }
}
