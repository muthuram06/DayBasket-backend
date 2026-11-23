package com.daybasket.dto.customer;

/**
 * Response DTO containing customer delivery address details.
 */
public record CustomerAddressResponse(

        Long id,
        String addressLine,
        String city,
        String state,
        String pincode,
        String landmark,
        Boolean isDefault

) {}