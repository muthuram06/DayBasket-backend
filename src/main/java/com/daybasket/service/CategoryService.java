
package com.daybasket.service;

import com.daybasket.dto.category.CategoryRequest;
import com.daybasket.dto.category.CategoryResponse;
import com.daybasket.entity.Category;
import com.daybasket.repository.CategoryRepository;
import com.daybasket.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {

        validateName(request.name());

        String slug = createSlug(
            request.slug() == null || request.slug().isBlank()
                ? request.name()
                : request.slug()
        );

        if (categoryRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Category slug already exists"
            );
        }

        Category category = new Category();

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setIsActive(
            request.isActive() == null ? true : request.isActive()
        );

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(
            Long id,
            CategoryRequest request) {

        Category category = findCategory(id);

        validateName(request.name());

        String slug = createSlug(
            request.slug() == null || request.slug().isBlank()
                ? request.name()
                : request.slug()
        );

        if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Category slug already exists"
            );
        }

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());

        if (request.isActive() != null) {
            category.setIsActive(request.isActive());
        }

        return mapToResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActive() {
        return categoryRepository
            .findAllByIsActiveTrueOrderByNameAsc()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return mapToResponse(findCategory(id));
    }

    @Transactional
    public void delete(Long id) {

        Category category = findCategory(id);

        if (productRepository.existsByCategoryId(id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot delete category containing products"
            );
        }

        categoryRepository.delete(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category not found"
            ));
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Category name is required"
            );
        }
    }

    private String createSlug(String value) {
        String slug = value.trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");

        if (slug.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid category slug"
            );
        }

        return slug;
    }

    private CategoryResponse mapToResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getImageUrl(),
            category.getIsActive(),
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
