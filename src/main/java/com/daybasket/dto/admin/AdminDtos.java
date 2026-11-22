
package com.daybasket.dto.admin;

import java.math.BigDecimal;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record VendorRequest(
        String companyName,
        String contactPerson,
        String email,
        String phone,
        String address
    ) {}

    public record DeliveryPartnerRequest(
        String fullName,
        String email,
        String phone,
        String password
    ) {}

    public record OrderStatusRequest(
        String status
    ) {}

    public record CustomerAssignmentRequest(
        Long customerId
    ) {}
}
