
package com.daybasket.repository;

import com.daybasket.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByCategoryId(Long categoryId);

    List<Product> findAllByIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findAllByCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(
            Long categoryId
    );

    @Query("""
        SELECT p FROM Product p
        JOIN FETCH p.category
        WHERE p.isActive = true
        AND (
            :keyword IS NULL
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        ORDER BY p.createdAt DESC
    """)
    List<Product> searchActiveProducts(
        @Param("keyword") String keyword,
        @Param("categoryId") Long categoryId
    );
}
