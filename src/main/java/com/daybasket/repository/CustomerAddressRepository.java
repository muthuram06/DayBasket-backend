package com.daybasket.repository;

import com.daybasket.entity.CustomerAddress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository
        extends JpaRepository<CustomerAddress, Long> {

    // Retrieve customer addresses with the default address first.
    List<CustomerAddress> findByCustomerIdOrderByIsDefaultDescCreatedAtDesc(
            Long customerId
    );

    // Find an address only when it belongs to the specified customer.
    Optional<CustomerAddress> findByIdAndCustomerId(
            Long id,
            Long customerId
    );

    // Reset all default addresses belonging to a customer.
    @Modifying
    @Query("""
            UPDATE CustomerAddress address
            SET address.isDefault = false
            WHERE address.customer.id = :customerId
            """)
    void clearDefaultAddress(
            @Param("customerId") Long customerId
    );
}