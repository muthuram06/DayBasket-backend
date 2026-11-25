package com.daybasket.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(

    Long id,

    String orderNumber,

    BigDecimal totalAmount,

    String status,

    String paymentMethod,

    String paymentStatus,

    String deliveryAddress,

    LocalDateTime createdAt,

    List<OrderItemResponse> items,

    String razorpayOrderId,

    String razorpayKeyId

) {}