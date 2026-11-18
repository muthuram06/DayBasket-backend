
package com.daybasket.dto.category;

public record CategoryRequest(
    String name,
    String slug,
    String description,
    String imageUrl,
    Boolean isActive
) {}
