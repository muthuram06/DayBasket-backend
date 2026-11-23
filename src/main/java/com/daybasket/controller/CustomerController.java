package com.daybasket.controller;

import com.daybasket.dto.customer.CustomerAddressRequest;
import com.daybasket.dto.customer.CustomerAddressResponse;
import com.daybasket.dto.customer.CustomerProfileResponse;
import com.daybasket.dto.customer.UpdateCustomerProfileRequest;
import com.daybasket.service.CustomerService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for authenticated customer profile
 * and delivery address operations.
 */
@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // --------------------------------------------------
    // CUSTOMER PROFILE ENDPOINTS
    // --------------------------------------------------

    /**
     * Retrieves the authenticated customer's profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<CustomerProfileResponse> getProfile(
            Authentication authentication) {

        CustomerProfileResponse response =
                customerService.getProfile(authentication.getName());

        return ResponseEntity.ok(response);
    }

    /**
     * Updates the authenticated customer's profile.
     */
    @PutMapping("/profile")
    public ResponseEntity<CustomerProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateCustomerProfileRequest request) {

        CustomerProfileResponse response =
                customerService.updateProfile(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------
    // CUSTOMER ADDRESS ENDPOINTS
    // --------------------------------------------------

    /**
     * Adds a new delivery address for the authenticated customer.
     */
    @PostMapping("/addresses")
    public ResponseEntity<CustomerAddressResponse> addAddress(
            Authentication authentication,
            @Valid @RequestBody CustomerAddressRequest request) {

        CustomerAddressResponse response =
                customerService.addAddress(
                        authentication.getName(),
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Retrieves all delivery addresses of the authenticated customer.
     */
    @GetMapping("/addresses")
    public ResponseEntity<List<CustomerAddressResponse>> getAddresses(
            Authentication authentication) {

        List<CustomerAddressResponse> response =
                customerService.getAddresses(authentication.getName());

        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing delivery address.
     */
    @PutMapping("/addresses/{id}")
    public ResponseEntity<CustomerAddressResponse> updateAddress(
            Authentication authentication,
            @PathVariable("id") Long addressId,
            @Valid @RequestBody CustomerAddressRequest request) {

        CustomerAddressResponse response =
                customerService.updateAddress(
                        authentication.getName(),
                        addressId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a delivery address belonging to the authenticated customer.
     */
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
            Authentication authentication,
            @PathVariable("id") Long addressId) {

        customerService.deleteAddress(
                authentication.getName(),
                addressId
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Sets an existing address as the default delivery address.
     */
    @PatchMapping("/addresses/{id}/default")
    public ResponseEntity<CustomerAddressResponse> setDefaultAddress(
            Authentication authentication,
            @PathVariable("id") Long addressId) {

        CustomerAddressResponse response =
                customerService.setDefaultAddress(
                        authentication.getName(),
                        addressId
                );

        return ResponseEntity.ok(response);
    }
}