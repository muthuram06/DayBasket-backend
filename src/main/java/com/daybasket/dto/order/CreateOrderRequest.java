package com.daybasket.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(

    @NotBlank(message = "Payment method is required")
    @Pattern(
        regexp = "COD|RAZORPAY",
        message = "Payment method must be COD or RAZORPAY"
    )
    String paymentMethod,

    @NotBlank(message = "Delivery address is required")
    @Size(max = 1000, message = "Delivery address is too long")
    String deliveryAddress,

    @NotEmpty(message = "Order must contain at least one item")
    List<@Valid OrderItemRequest> items

) {}