package com.daybasket.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a delivery address.
 */
public record CustomerAddressRequest(

        @NotBlank(message = "Address line is required")
        @Size(max = 255, message = "Address line cannot exceed 255 characters")
        String addressLine,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        String city,

        @NotBlank(message = "State is required")
        @Size(max = 100, message = "State cannot exceed 100 characters")
        String state,

        @NotBlank(message = "Pincode is required")
        @Size(max = 10, message = "Pincode cannot exceed 10 characters")
        String pincode,

        @Size(max = 150, message = "Landmark cannot exceed 150 characters")
        String landmark,

        Boolean isDefault

) {}