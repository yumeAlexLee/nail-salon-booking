package com.nailsalon.booking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nailsalon.booking.entity.MenuItem;
import com.nailsalon.booking.mapper.MenuItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemMapper menuItemMapper;

    /**
     * 获取所有启用的菜单项目，按分类分组，分类内按 sort_order 排序
     *
     * 📖 知识点：流式处理
     * .stream().collect(Collectors.groupingBy(...))
     * 把 List 按 category 字段分组，变成 Map<分类, List<项目>>
     */
    public Map<String, List<MenuItem>> getActiveMenuGrouped() {
        List<MenuItem> items = menuItemMapper.selectList(
                new LambdaQueryWrapper<MenuItem>()
                        .eq(MenuItem::getIsActive, 1)
                        .orderByAsc(MenuItem::getSortOrder)
        );

        // 按 category 分组，保持每组的排序
        return items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getCategoryJa() != null ? item.getCategory() + "|" + item.getCategoryJa() : item.getCategory(),
                        Collectors.toList()
                ));
    }

    /** 获取所有菜单项目（含隐藏的），按排序排列 */
    public List<MenuItem> getAllItems() {
        return menuItemMapper.selectList(
                new LambdaQueryWrapper<MenuItem>().orderByAsc(MenuItem::getSortOrder)
        );
    }

    /** 获取单个菜单项目 */
    public MenuItem getItem(Long id) {
        return menuItemMapper.selectById(id);
    }

    /** 新增菜单项目 */
    @Transactional
    public void createItem(MenuItem item) {
        menuItemMapper.insert(item);
    }

    /** 更新菜单项目 */
    @Transactional
    public void updateItem(MenuItem item) {
        menuItemMapper.updateById(item);
    }

    /** 删除菜单项目 */
    @Transactional
    public void deleteItem(Long id) {
        menuItemMapper.deleteById(id);
    }
}
