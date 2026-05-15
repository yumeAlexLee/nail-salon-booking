package com.nailsalon.booking.controller;

import com.nailsalon.booking.dto.ApiResponse;
import com.nailsalon.booking.entity.MenuItem;
import com.nailsalon.booking.entity.MenuItemOption;
import com.nailsalon.booking.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    /** 获取所有启用的菜单（按分类分组）— 给客人看 */
    @GetMapping("/active")
    public ApiResponse<Map<String, List<MenuItem>>> getActiveMenu() {
        return ApiResponse.success(menuService.getActiveMenuGrouped());
    }

    /** 获取所有菜单项目（含隐藏的）— 给管理后台用 */
    @GetMapping("/admin/list")
    public ApiResponse<List<MenuItem>> getAllItems() {
        return ApiResponse.success(menuService.getAllItems());
    }

    /** 获取单个项目 */
    @GetMapping("/admin/{id}")
    public ApiResponse<MenuItem> getItem(@PathVariable Long id) {
        return ApiResponse.success(menuService.getItem(id));
    }

    /** 新增菜单项目 */
    @PostMapping("/admin")
    public ApiResponse<Void> createItem(@RequestBody MenuItem item) {
        menuService.createItem(item);
        return ApiResponse.success(null);
    }

    /** 更新菜单项目 */
    @PutMapping("/admin/{id}")
    public ApiResponse<Void> updateItem(@PathVariable Long id, @RequestBody MenuItem item) {
        item.setId(id);
        menuService.updateItem(item);
        return ApiResponse.success(null);
    }

    /** 删除菜单项目 */
    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable Long id) {
        menuService.deleteItem(id);
        return ApiResponse.success(null);
    }

    // ─────────────── 子选项 API ───────────────

    /** 获取某个菜单项目的启用子选项（给客人看） */
    @GetMapping("/{menuItemId}/options")
    public ApiResponse<List<MenuItemOption>> getActiveOptions(@PathVariable Long menuItemId) {
        return ApiResponse.success(menuService.getActiveOptions(menuItemId));
    }

    /** 批量查询子选项详情（给客人看，用逗号分隔的ID） */
    @GetMapping("/options/batch")
    public ApiResponse<List<MenuItemOption>> getOptionsBatch(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong)
                .collect(Collectors.toList());
        return ApiResponse.success(menuService.getOptionsByIds(idList));
    }

    /** 获取某个菜单项目的所有子选项（后台用） */
    @GetMapping("/admin/options/list")
    public ApiResponse<List<MenuItemOption>> getAllOptions(@RequestParam Long menuItemId) {
        return ApiResponse.success(menuService.getAllOptions(menuItemId));
    }

    /** 新增子选项（后台） */
    @PostMapping("/admin/options")
    public ApiResponse<Void> createOption(@RequestBody MenuItemOption option) {
        menuService.createOption(option);
        return ApiResponse.success(null);
    }

    /** 更新子选项（后台） */
    @PutMapping("/admin/options/{id}")
    public ApiResponse<Void> updateOption(@PathVariable Long id, @RequestBody MenuItemOption option) {
        option.setId(id);
        menuService.updateOption(option);
        return ApiResponse.success(null);
    }

    /** 删除子选项（后台） */
    @DeleteMapping("/admin/options/{id}")
    public ApiResponse<Void> deleteOption(@PathVariable Long id) {
        menuService.deleteOption(id);
        return ApiResponse.success(null);
    }
}
