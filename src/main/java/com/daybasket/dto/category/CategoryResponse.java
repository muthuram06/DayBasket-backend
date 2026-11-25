package com.daybasket.dto.category;

import java.time.LocalDateTime;

public record CategoryResponse(
    Long id,
    String name,
    String slug,
    String description,
    String imageUrl,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}