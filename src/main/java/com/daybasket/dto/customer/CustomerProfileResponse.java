package com.daybasket.dto.customer;

/**
 * Response DTO containing the authenticated customer's profile details.
 */
public record CustomerProfileResponse(

    Long id,
    String fullName,
    String email,
    String phone

) {}