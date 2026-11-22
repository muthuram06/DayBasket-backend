
package com.daybasket.controller.admin;

import com.daybasket.dto.admin.AdminDtos.DeliveryPartnerRequest;
import com.daybasket.service.admin.AdminService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/delivery")
public class AdminDeliveryController {

    private final AdminService adminService;

    public AdminDeliveryController(
            AdminService adminService) {
        this.adminService = adminService;
    }

    // List all delivery partners
    @GetMapping("/partners")
    public List<Map<String, Object>> getPartners() {
        return adminService.getDeliveryPartners();
    }

    // Create a delivery partner account
    @PostMapping("/partners")
    public Map<String, Object> createPartner(
            @RequestBody DeliveryPartnerRequest request) {
        return adminService.createDeliveryPartner(request);
    }

    // Activate or deactivate a delivery partner
    @PatchMapping("/partners/{partnerId}/status")
    public Map<String, String> updatePartnerStatus(
            @PathVariable Long partnerId,
            @RequestParam boolean active) {

        return Map.of(
            "message",
            adminService.updateDeliveryPartnerStatus(
                partnerId, active
            )
        );
    }

    // Assign a customer to a delivery partner
    @PostMapping("/partners/{partnerId}/customers/{customerId}")
    public Map<String, String> assignCustomer(
            @PathVariable Long partnerId,
            @PathVariable Long customerId) {

        return Map.of(
            "message",
            adminService.assignCustomer(
                partnerId, customerId
            )
        );
    }

    // View customers assigned to a partner
    @GetMapping("/partners/{partnerId}/customers")
    public List<Map<String, Object>> getAssignedCustomers(
            @PathVariable Long partnerId) {

        return adminService.getAssignedCustomers(partnerId);
    }

    // Remove customer assignment
    @DeleteMapping("/partners/{partnerId}/customers/{customerId}")
    public Map<String, String> removeAssignment(
            @PathVariable Long partnerId,
            @PathVariable Long customerId) {

        return Map.of(
            "message",
            adminService.removeCustomerAssignment(
                partnerId, customerId
            )
        );
    }
}
