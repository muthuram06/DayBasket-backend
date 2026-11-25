package com.daybasket.repository;

import com.daybasket.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    Optional<Order> findByOrderNumberAndCustomerId(
        String orderNumber,
        Long customerId
    );

    boolean existsByOrderNumber(String orderNumber);
}