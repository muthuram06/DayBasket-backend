
package com.daybasket.controller.admin;

import com.daybasket.service.admin.AdminService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/customers")
public class AdminCustomerController {

    private final AdminService adminService;

    public AdminCustomerController(
            AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<Map<String, Object>> getCustomers(
            @RequestParam(required = false) String search) {

        return adminService.getCustomers(search);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getCustomer(
            @PathVariable Long id) {

        return adminService.getCustomer(id);
    }

    @PatchMapping("/{id}/status")
    public Map<String, String> updateStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        return Map.of(
            "message",
            adminService.updateCustomerStatus(id, active)
        );
    }
}
