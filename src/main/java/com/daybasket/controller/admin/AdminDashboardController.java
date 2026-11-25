package com.daybasket.controller.admin;

import com.daybasket.service.admin.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminService adminService;

    public AdminDashboardController(
            AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public Map<String, Object> getDashboard() {
        return adminService.getDashboard();
    }
}