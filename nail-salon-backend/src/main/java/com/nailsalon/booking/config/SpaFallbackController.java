package com.nailsalon.booking.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA 路由回退：所有非 API 的非文件请求都指向 index.html
 * 让 Vue Router 的 createWebHistory() 正常运作
 */
@Controller
public class SpaFallbackController {

    @RequestMapping(value = {
            "/",
            "/menu",
            "/book",
            "/form",
            "/success",
            "/guide",
            "/portfolio",
            "/my-bookings",
            "/admin"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
