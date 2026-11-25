package com.daybasket.controller.admin;

import com.daybasket.dto.admin.AdminDtos.OrderStatusRequest;
import com.daybasket.service.admin.AdminService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminService adminService;

    public AdminOrderController(
            AdminService adminService) {
        this.adminService = adminService;
    }

    // Get all orders or filter by status
    @GetMapping
    public List<Map<String, Object>> getOrders(
            @RequestParam(required = false) String status) {

        return adminService.getOrders(status);
    }

    // Update order status
    @PatchMapping("/{orderId}/status")
    public Map<String, String> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusRequest request) {

        return Map.of(
            "message",
            adminService.updateOrderStatus(
                orderId, request.status()
            )
        );
    }

    // Assign delivery partner to an order
    @PatchMapping("/{orderId}/assign/{partnerId}")
    public Map<String, String> assignDeliveryPartner(
            @PathVariable Long orderId,
            @PathVariable Long partnerId) {

        return Map.of(
            "message",
            adminService.assignOrderToPartner(
                orderId, partnerId
            )
        );
    }
}