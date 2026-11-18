
package com.daybasket.controller;

import com.daybasket.dto.product.ProductRequest;
import com.daybasket.dto.product.ProductResponse;
import com.daybasket.service.ProductService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // CUSTOMER: Browse active products
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getProducts() {
        return ResponseEntity.ok(
            productService.getAllActive()
        );
    }

    // CUSTOMER: Search products
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId) {

        return ResponseEntity.ok(
            productService.search(keyword, categoryId)
        );
    }

    // CUSTOMER: Products by category
    @GetMapping("/products/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId) {

        return ResponseEntity.ok(
            productService.getByCategory(categoryId)
        );
    }

    // CUSTOMER: Product details
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            productService.getById(id)
        );
    }

    // ADMIN: View all products including inactive products
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        return ResponseEntity.ok(
            productService.getAll()
        );
    }

    // ADMIN: Create product
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/products")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody ProductRequest request) {

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(productService.create(request));
    }

    // ADMIN: Update product
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {

        return ResponseEntity.ok(
            productService.update(id, request)
        );
    }

    // ADMIN: Delete product
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(
            @PathVariable Long id) {

        productService.delete(id);

        return ResponseEntity.ok(
            Map.of("message", "Product deleted successfully")
        );
    }
}
