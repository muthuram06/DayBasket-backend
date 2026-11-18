
package com.daybasket.controller;

import com.daybasket.dto.category.CategoryRequest;
import com.daybasket.dto.category.CategoryResponse;
import com.daybasket.service.CategoryService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // CUSTOMER: View active categories
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories() {
        return ResponseEntity.ok(
            categoryService.getAllActive()
        );
    }

    // CUSTOMER: View category details
    @GetMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> getCategory(
            @PathVariable Long id) {
        return ResponseEntity.ok(
            categoryService.getById(id)
        );
    }

    // ADMIN: View all categories
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/categories")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(
            categoryService.getAll()
        );
    }

    // ADMIN: Create category
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @RequestBody CategoryRequest request) {

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(categoryService.create(request));
    }

    // ADMIN: Update category
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {

        return ResponseEntity.ok(
            categoryService.update(id, request)
        );
    }

    // ADMIN: Delete category
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/categories/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(
            @PathVariable Long id) {

        categoryService.delete(id);

        return ResponseEntity.ok(
            Map.of("message", "Category deleted successfully")
        );
    }
}
