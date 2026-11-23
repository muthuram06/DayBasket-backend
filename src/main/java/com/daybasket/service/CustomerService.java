package com.daybasket.service;

import com.daybasket.dto.customer.CustomerAddressRequest;
import com.daybasket.dto.customer.CustomerAddressResponse;
import com.daybasket.dto.customer.CustomerProfileResponse;
import com.daybasket.dto.customer.UpdateCustomerProfileRequest;

import com.daybasket.entity.Customer;
import com.daybasket.entity.CustomerAddress;
import com.daybasket.entity.User;

import com.daybasket.repository.CustomerAddressRepository;
import com.daybasket.repository.CustomerRepository;
import com.daybasket.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Handles customer profile and delivery address operations.
 */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerAddressRepository customerAddressRepository,
            UserRepository userRepository) {

        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.userRepository = userRepository;
    }

    // --------------------------------------------------
    // CUSTOMER PROFILE OPERATIONS
    // --------------------------------------------------

    /**
     * Retrieves the profile of the authenticated customer.
     */
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfile(String email) {

        User user = findUserByEmail(email);
        Customer customer = findCustomerByUserId(user.getId());

        return mapToProfileResponse(customer, user);
    }

    /**
     * Updates the authenticated customer's name and phone number.
     */
    @Transactional
    public CustomerProfileResponse updateProfile(
            String email,
            UpdateCustomerProfileRequest request) {

        User user = findUserByEmail(email);

        String updatedPhone = normalizePhone(request.phone());

        // Prevent assigning a phone number already used by another user.
        if (updatedPhone != null
                && !Objects.equals(updatedPhone, user.getPhone())
                && userRepository.existsByPhone(updatedPhone)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phone number is already registered"
            );
        }

        user.setFullName(request.fullName().trim());
        user.setPhone(updatedPhone);

        User savedUser = userRepository.save(user);

        Customer customer = findCustomerByUserId(savedUser.getId());

        return mapToProfileResponse(customer, savedUser);
    }

    // --------------------------------------------------
    // CUSTOMER ADDRESS OPERATIONS
    // --------------------------------------------------

    /**
     * Creates a delivery address for the authenticated customer.
     */
    @Transactional
    public CustomerAddressResponse addAddress(
            String email,
            CustomerAddressRequest request) {

        Customer customer = findCustomerByEmail(email);

        List<CustomerAddress> existingAddresses =
                customerAddressRepository
                        .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(
                                customer.getId()
                        );

        CustomerAddress address = new CustomerAddress();

        address.setCustomer(customer);
        address.setAddressLine(request.addressLine().trim());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPincode(request.pincode().trim());
        address.setLandmark(normalizeLandmark(request.landmark()));
        address.setCreatedAt(LocalDateTime.now());

        // The first saved address becomes the default automatically.
        boolean makeDefault =
                Boolean.TRUE.equals(request.isDefault())
                        || existingAddresses.isEmpty();

        if (makeDefault) {
            customerAddressRepository.clearDefaultAddress(customer.getId());
        }

        address.setIsDefault(makeDefault);

        CustomerAddress savedAddress =
                customerAddressRepository.save(address);

        return mapToAddressResponse(savedAddress);
    }

    /**
     * Retrieves all delivery addresses of the authenticated customer.
     */
    @Transactional(readOnly = true)
    public List<CustomerAddressResponse> getAddresses(String email) {

        Customer customer = findCustomerByEmail(email);

        return customerAddressRepository
                .findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(
                        customer.getId()
                )
                .stream()
                .map(this::mapToAddressResponse)
                .toList();
    }

    /**
     * Updates an address only if it belongs to the authenticated customer.
     */
    @Transactional
    public CustomerAddressResponse updateAddress(
            String email,
            Long addressId,
            CustomerAddressRequest request) {

        Customer customer = findCustomerByEmail(email);

        CustomerAddress address = findAddressByIdAndCustomerId(
                addressId,
                customer.getId()
        );

        address.setAddressLine(request.addressLine().trim());
        address.setCity(request.city().trim());
        address.setState(request.state().trim());
        address.setPincode(request.pincode().trim());
        address.setLandmark(normalizeLandmark(request.landmark()));

        if (Boolean.TRUE.equals(request.isDefault())) {

            customerAddressRepository.clearDefaultAddress(customer.getId());
            address.setIsDefault(true);

        } else if (Boolean.FALSE.equals(request.isDefault())) {

            address.setIsDefault(false);
        }

        CustomerAddress savedAddress =
                customerAddressRepository.save(address);

        return mapToAddressResponse(savedAddress);
    }

    /**
     * Deletes an address only if it belongs to the authenticated customer.
     */
    @Transactional
    public void deleteAddress(String email, Long addressId) {

        Customer customer = findCustomerByEmail(email);

        CustomerAddress address = findAddressByIdAndCustomerId(
                addressId,
                customer.getId()
        );

        customerAddressRepository.delete(address);
    }

    /**
     * Sets an address as the default delivery address.
     */
    @Transactional
    public CustomerAddressResponse setDefaultAddress(
            String email,
            Long addressId) {

        Customer customer = findCustomerByEmail(email);

        CustomerAddress address = findAddressByIdAndCustomerId(
                addressId,
                customer.getId()
        );

        // Clear the previous default before assigning the new one.
        customerAddressRepository.clearDefaultAddress(customer.getId());

        address.setIsDefault(true);

        CustomerAddress savedAddress =
                customerAddressRepository.save(address);

        return mapToAddressResponse(savedAddress);
    }

    // --------------------------------------------------
    // COMMON HELPER METHODS
    // --------------------------------------------------

    /**
     * Finds a user using the authenticated email address.
     */
    private User findUserByEmail(String email) {

        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    /**
     * Finds the customer profile associated with a user.
     */
    private Customer findCustomerByUserId(Long userId) {

        return customerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Customer profile not found"
                ));
    }

    /**
     * Finds a customer using the authenticated email address.
     */
    private Customer findCustomerByEmail(String email) {

        User user = findUserByEmail(email);

        return findCustomerByUserId(user.getId());
    }

    /**
     * Finds an address only when it belongs to the specified customer.
     */
    private CustomerAddress findAddressByIdAndCustomerId(
            Long addressId,
            Long customerId) {

        return customerAddressRepository
                .findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery address not found"
                ));
    }

    /**
     * Converts blank phone values into null.
     */
    private String normalizePhone(String phone) {

        if (phone == null || phone.isBlank()) {
            return null;
        }

        return phone.trim();
    }

    /**
     * Converts blank landmark values into null.
     */
    private String normalizeLandmark(String landmark) {

        if (landmark == null || landmark.isBlank()) {
            return null;
        }

        return landmark.trim();
    }

    /**
     * Maps entity data into the customer profile response DTO.
     */
    private CustomerProfileResponse mapToProfileResponse(
            Customer customer,
            User user) {

        return new CustomerProfileResponse(
                customer.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone()
        );
    }

    /**
     * Maps entity data into the customer address response DTO.
     */
    private CustomerAddressResponse mapToAddressResponse(
            CustomerAddress address) {

        return new CustomerAddressResponse(
                address.getId(),
                address.getAddressLine(),
                address.getCity(),
                address.getState(),
                address.getPincode(),
                address.getLandmark(),
                address.getIsDefault()
        );
    }
}