package com.daybasket.dto.product;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
    Long categoryId,
    String name,
    String slug,
    String sku,
    String description,
    String brand,
    BigDecimal price,
    BigDecimal discountPrice,
    Integer stockQuantity,
    String unit,
    Boolean isActive,
    List<String> imageUrls
) {}