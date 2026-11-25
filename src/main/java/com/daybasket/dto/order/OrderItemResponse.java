package com.daybasket.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    Long productId,
    String productName,
    String productSku,
    Integer quantity,
    String unit,
    BigDecimal unitPrice,
    BigDecimal subtotal
) {}