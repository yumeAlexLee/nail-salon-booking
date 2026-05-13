package com.nailsalon.booking.controller;

import com.nailsalon.booking.dto.ApiResponse;
import com.nailsalon.booking.entity.MenuItem;
import com.nailsalon.booking.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
