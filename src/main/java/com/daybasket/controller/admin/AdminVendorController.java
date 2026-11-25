package com.daybasket.controller.admin;

import com.daybasket.dto.admin.AdminDtos.VendorRequest;
import com.daybasket.service.admin.AdminService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/vendors")
public class AdminVendorController {

    private final AdminService adminService;

    public AdminVendorController(
            AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<Map<String, Object>> getVendors() {
        return adminService.getVendors();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getVendor(
            @PathVariable Long id) {
        return adminService.getVendor(id);
    }

    @PostMapping
    public Map<String, Object> createVendor(
            @RequestBody VendorRequest request) {
        return adminService.createVendor(request);
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateVendor(
            @PathVariable Long id,
            @RequestBody VendorRequest request) {
        return adminService.updateVendor(id, request);
    }

    @PatchMapping("/{id}/status")
    public Map<String, String> updateVendorStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {

        return Map.of(
            "message",
            adminService.updateVendorStatus(id, active)
        );
    }
}