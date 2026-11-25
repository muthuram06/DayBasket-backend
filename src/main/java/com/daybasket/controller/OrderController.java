package com.daybasket.controller;

import com.daybasket.dto.order.CreateOrderRequest;
import com.daybasket.dto.order.OrderResponse;
import com.daybasket.dto.order.PaymentVerifyRequest;
import com.daybasket.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // Create COD or Razorpay order.
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.createOrder(
            authentication.getName(),
            request
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Verify Razorpay payment.
    @PostMapping("/verify-payment")
    public ResponseEntity<OrderResponse> verifyPayment(
            Authentication authentication,
            @Valid @RequestBody PaymentVerifyRequest request) {

        OrderResponse response = orderService.verifyPayment(
            authentication.getName(),
            request
        );

        return ResponseEntity.ok(response);
    }

    // Get all orders belonging to the logged-in customer.
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            Authentication authentication) {

        return ResponseEntity.ok(
            orderService.getMyOrders(authentication.getName())
        );
    }

    // Get one order using its order number.
    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByNumber(
            Authentication authentication,
            @PathVariable String orderNumber) {

        return ResponseEntity.ok(
            orderService.getOrderByNumber(
                authentication.getName(),
                orderNumber
            )
        );
    }
}