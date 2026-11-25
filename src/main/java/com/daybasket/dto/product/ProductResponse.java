package com.daybasket.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String name,
    String slug,
    String sku,
    String description,
    String brand,
    BigDecimal price,
    BigDecimal discountPrice,
    BigDecimal finalPrice,
    Integer stockQuantity,
    String unit,
    Boolean isActive,
    List<String> imageUrls,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}