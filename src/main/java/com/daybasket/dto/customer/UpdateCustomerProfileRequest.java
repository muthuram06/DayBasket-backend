package com.daybasket.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating the authenticated customer's profile.
 */
public record UpdateCustomerProfileRequest(

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    String fullName,

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    String phone

) {}