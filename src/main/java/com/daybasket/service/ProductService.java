package com.daybasket.service;

import com.daybasket.dto.product.ProductRequest;
import com.daybasket.dto.product.ProductResponse;

import com.daybasket.entity.Category;
import com.daybasket.entity.Product;
import com.daybasket.entity.ProductImage;

import com.daybasket.repository.CategoryRepository;
import com.daybasket.repository.ProductRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {

        validateRequest(request);

        Category category = findCategory(request.categoryId());

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot add products to an inactive category"
            );
        }

        String slug = createSlug(
            request.slug() == null || request.slug().isBlank()
                ? request.name()
                : request.slug()
        );

        if (productRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product slug already exists"
            );
        }

        if (productRepository.existsBySku(request.sku().trim())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product SKU already exists"
            );
        }

        Product product = new Product();

        product.setCategory(category);
        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setSku(request.sku().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setDiscountPrice(request.discountPrice());
        product.setStockQuantity(request.stockQuantity() == null
            ? 0 : request.stockQuantity());
        product.setUnit(request.unit().trim());
        product.setIsActive(request.isActive() == null
            ? true : request.isActive());

        addImages(product, request.imageUrls());

        return mapToResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(
            Long id,
            ProductRequest request) {

        validateRequest(request);

        Product product = findProduct(id);
        Category category = findCategory(request.categoryId());

        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Cannot assign product to an inactive category"
            );
        }

        String slug = createSlug(
            request.slug() == null || request.slug().isBlank()
                ? request.name()
                : request.slug()
        );

        if (productRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product slug already exists"
            );
        }

        if (productRepository.existsBySkuAndIdNot(
                request.sku().trim(), id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product SKU already exists"
            );
        }

        product.setCategory(category);
        product.setName(request.name().trim());
        product.setSlug(slug);
        product.setSku(request.sku().trim());
        product.setDescription(request.description());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setDiscountPrice(request.discountPrice());
        product.setStockQuantity(request.stockQuantity() == null
            ? 0 : request.stockQuantity());
        product.setUnit(request.unit().trim());

        if (request.isActive() != null) {
            product.setIsActive(request.isActive());
        }

        product.clearImages();
        addImages(product, request.imageUrls());

        return mapToResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActive() {
        return productRepository
            .findAllByIsActiveTrueOrderByCreatedAtDesc()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository
            .findAllByOrderByCreatedAtDesc()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getByCategory(Long categoryId) {

        return productRepository
            .findAllByCategoryIdAndIsActiveTrueOrderByCreatedAtDesc(
                categoryId
            )
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> search(
            String keyword,
            Long categoryId) {

        String normalizedKeyword =
            keyword == null || keyword.isBlank()
                ? null : keyword.trim();

        return productRepository
            .searchActiveProducts(normalizedKeyword, categoryId)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {

        Product product = findProduct(id);

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Product not found"
            );
        }

        return mapToResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Product not found"
            ));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Category not found"
            ));
    }

    private void validateRequest(ProductRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Product request is required"
            );
        }

        if (request.categoryId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Category is required"
            );
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Product name is required"
            );
        }

        if (request.sku() == null || request.sku().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Product SKU is required"
            );
        }

        if (request.unit() == null || request.unit().isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Product unit is required"
            );
        }

        if (request.price() == null ||
                request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Product price must be greater than zero"
            );
        }

        if (request.discountPrice() != null &&
                (request.discountPrice().compareTo(BigDecimal.ZERO) < 0 ||
                 request.discountPrice().compareTo(request.price()) >= 0)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Discount price must be zero or greater and less than price"
            );
        }

        if (request.stockQuantity() != null &&
                request.stockQuantity() < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Stock quantity cannot be negative"
            );
        }
    }

    private void addImages(
            Product product,
            List<String> imageUrls) {

        if (imageUrls == null) {
            return;
        }

        int order = 0;

        for (String url : imageUrls) {

            if (url == null || url.isBlank()) {
                continue;
            }

            ProductImage image = new ProductImage();

            image.setImageUrl(url.trim());
            image.setSortOrder(order++);

            product.addImage(image);
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
                "Invalid product slug"
            );
        }

        return slug;
    }

    private ProductResponse mapToResponse(Product product) {

        BigDecimal finalPrice =
            product.getDiscountPrice() != null
                ? product.getDiscountPrice()
                : product.getPrice();

        List<String> imageUrls = product.getImages()
            .stream()
            .map(ProductImage::getImageUrl)
            .toList();

        return new ProductResponse(
            product.getId(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getName(),
            product.getSlug(),
            product.getSku(),
            product.getDescription(),
            product.getBrand(),
            product.getPrice(),
            product.getDiscountPrice(),
            finalPrice,
            product.getStockQuantity(),
            product.getUnit(),
            product.getIsActive(),
            imageUrls,
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}