package com.daybasket.service.admin;

import com.daybasket.dto.admin.AdminDtos.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

@Service
public class AdminService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    private static final Set<String> ORDER_STATUSES = Set.of(
        "PLACED",
        "CONFIRMED",
        "PACKED",
        "OUT_FOR_DELIVERY",
        "DELIVERED",
        "CANCELLED"
    );

    public AdminService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {

        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // 1. ADMIN DASHBOARD
    // ==========================================

    public Map<String, Object> getDashboard() {

        Long customers = count(
            "SELECT COUNT(*) FROM customers"
        );

        Long vendors = count(
            "SELECT COUNT(*) FROM vendors WHERE is_active = TRUE"
        );

        Long deliveryPartners = count("""
            SELECT COUNT(*)
            FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE r.name = 'DELIVERY_PARTNER'
            AND u.is_active = TRUE
            """);

        Long totalOrders = count(
            "SELECT COUNT(*) FROM orders"
        );

        Long pendingOrders = count("""
            SELECT COUNT(*) FROM orders
            WHERE status IN ('PLACED', 'CONFIRMED', 'PACKED')
            """);

        Long deliveredOrders = count("""
            SELECT COUNT(*) FROM orders
            WHERE status = 'DELIVERED'
            """);

        Double revenue = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(total_amount), 0)
            FROM orders
            WHERE status = 'DELIVERED'
            """, Double.class);

        return Map.of(
            "totalCustomers", customers,
            "totalVendors", vendors,
            "activeDeliveryPartners", deliveryPartners,
            "totalOrders", totalOrders,
            "pendingOrders", pendingOrders,
            "deliveredOrders", deliveredOrders,
            "totalRevenue", revenue == null ? 0.0 : revenue
        );
    }

    private Long count(String sql) {
        Long result = jdbcTemplate.queryForObject(
            sql, Long.class
        );
        return result == null ? 0L : result;
    }

    // ==========================================
    // 2. CUSTOMER MANAGEMENT
    // ==========================================

    public List<Map<String, Object>> getCustomers(String search) {

        String keyword = search == null ? "" : search.trim();

        return jdbcTemplate.queryForList("""
            SELECT
                c.id AS customerId,
                u.id AS userId,
                u.full_name AS fullName,
                u.email,
                u.phone,
                u.is_active AS active,
                c.created_at AS registeredAt
            FROM customers c
            JOIN users u ON c.user_id = u.id
            WHERE (
                LOWER(u.full_name) LIKE LOWER(?)
                OR LOWER(u.email) LIKE LOWER(?)
                OR COALESCE(u.phone, '') LIKE ?
            )
            ORDER BY c.created_at DESC
            """,
            "%" + keyword + "%",
            "%" + keyword + "%",
            "%" + keyword + "%"
        );
    }

    public Map<String, Object> getCustomer(Long id) {

        List<Map<String, Object>> result =
            jdbcTemplate.queryForList("""
                SELECT
                    c.id AS customerId,
                    u.id AS userId,
                    u.full_name AS fullName,
                    u.email,
                    u.phone,
                    u.is_active AS active,
                    c.created_at AS registeredAt
                FROM customers c
                JOIN users u ON c.user_id = u.id
                WHERE c.id = ?
                """, id);

        if (result.isEmpty()) {
            throw notFound("Customer not found");
        }

        return result.get(0);
    }

    @Transactional
    public String updateCustomerStatus(Long customerId, boolean active) {

        int updated = jdbcTemplate.update("""
            UPDATE users
            SET is_active = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = (
                SELECT user_id FROM customers WHERE id = ?
            )
            """, active, customerId);

        if (updated == 0) {
            throw notFound("Customer not found");
        }

        return active
            ? "Customer activated successfully"
            : "Customer deactivated successfully";
    }

    // ==========================================
    // 3. VENDOR MANAGEMENT
    // ==========================================

    public List<Map<String, Object>> getVendors() {

        return jdbcTemplate.queryForList("""
            SELECT *
            FROM vendors
            ORDER BY created_at DESC
            """);
    }

    public Map<String, Object> getVendor(Long id) {

        List<Map<String, Object>> result =
            jdbcTemplate.queryForList(
                "SELECT * FROM vendors WHERE id = ?", id
            );

        if (result.isEmpty()) {
            throw notFound("Vendor not found");
        }

        return result.get(0);
    }

    @Transactional
    public Map<String, Object> createVendor(VendorRequest request) {

        validateVendor(request);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps =
                connection.prepareStatement("""
                    INSERT INTO vendors (
                        company_name,
                        contact_person,
                        email,
                        phone,
                        address,
                        is_active,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, TRUE,
                            CURRENT_TIMESTAMP,
                            CURRENT_TIMESTAMP)
                    """,
                    new String[] {"id"}
                );

            ps.setString(1, request.companyName().trim());
            ps.setString(2, request.contactPerson());
            ps.setString(3, request.email());
            ps.setString(4, request.phone());
            ps.setString(5, request.address());

            return ps;

        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id == null) {
            throw new IllegalStateException(
                "Vendor was created, but its generated ID could not be retrieved"
            );
        }

        return getVendor(id.longValue());
    }

    @Transactional
    public Map<String, Object> updateVendor(
            Long id,
            VendorRequest request) {

        validateVendor(request);

        int updated = jdbcTemplate.update("""
            UPDATE vendors
            SET company_name = ?,
                contact_person = ?,
                email = ?,
                phone = ?,
                address = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            request.companyName().trim(),
            request.contactPerson(),
            request.email(),
            request.phone(),
            request.address(),
            id
        );

        if (updated == 0) {
            throw notFound("Vendor not found");
        }

        return getVendor(id);
    }

    @Transactional
    public String updateVendorStatus(Long id, boolean active) {

        int updated = jdbcTemplate.update("""
            UPDATE vendors
            SET is_active = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """, active, id);

        if (updated == 0) {
            throw notFound("Vendor not found");
        }

        return active
            ? "Vendor activated successfully"
            : "Vendor deactivated successfully";
    }

    private void validateVendor(VendorRequest request) {

        if (request == null ||
                request.companyName() == null ||
                request.companyName().isBlank()) {

            throw badRequest("Company name is required");
        }
    }

    // ==========================================
    // 4. DELIVERY PARTNER MANAGEMENT
    // ==========================================

    public List<Map<String, Object>> getDeliveryPartners() {

        return jdbcTemplate.queryForList("""
            SELECT
                u.id AS userId,
                u.full_name AS fullName,
                u.email,
                u.phone,
                u.is_active AS active,
                u.created_at AS registeredAt,
                COUNT(dpc.customer_id) AS assignedCustomers
            FROM users u
            JOIN roles r ON u.role_id = r.id
            LEFT JOIN delivery_partner_customers dpc
                ON dpc.delivery_partner_user_id = u.id
            WHERE r.name = 'DELIVERY_PARTNER'
            GROUP BY u.id
            ORDER BY u.created_at DESC
            """);
    }

    @Transactional
    public Map<String, Object> createDeliveryPartner(
            DeliveryPartnerRequest request) {

        if (request == null ||
                request.fullName() == null ||
                request.fullName().isBlank() ||
                request.email() == null ||
                request.email().isBlank() ||
                request.password() == null ||
                request.password().length() < 8) {

            throw badRequest(
                "Name, email and password (minimum 8 characters) are required"
            );
        }

        String email = request.email()
            .trim()
            .toLowerCase(Locale.ROOT);

        Long existing = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM users WHERE email = ?
            """, Long.class, email);

        if (existing != null && existing > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Email is already registered"
            );
        }

        if (request.phone() != null &&
                !request.phone().isBlank()) {

            Long phoneExists = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM users WHERE phone = ?
                """, Long.class, request.phone().trim());

            if (phoneExists != null && phoneExists > 0) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Phone number is already registered"
                );
            }
        }

        String encodedPassword =
            passwordEncoder.encode(request.password());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps =
                connection.prepareStatement("""
                    INSERT INTO users (
                        full_name,
                        email,
                        phone,
                        password_hash,
                        role_id,
                        is_active,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        ?, ?, ?, ?,
                        (SELECT id FROM roles
                         WHERE name = 'DELIVERY_PARTNER'),
                        TRUE,
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP
                    )
                    """,
                    new String[] {"id"}
                );

            ps.setString(1, request.fullName().trim());
            ps.setString(2, email);
            ps.setString(
                3,
                request.phone() == null ||
                request.phone().isBlank()
                    ? null : request.phone().trim()
            );
            ps.setString(4, encodedPassword);

            return ps;

        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id == null) {
            throw new IllegalStateException(
                "Delivery partner was created, but its generated ID could not be retrieved"
            );
        }

        return jdbcTemplate.queryForMap("""
            SELECT
                id AS userId,
                full_name AS fullName,
                email,
                phone,
                is_active AS active,
                created_at AS registeredAt
            FROM users
            WHERE id = ?
            """, id.longValue());
    }

    @Transactional
    public String updateDeliveryPartnerStatus(
            Long partnerId,
            boolean active) {

        int updated = jdbcTemplate.update("""
            UPDATE users
            SET is_active = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            AND role_id = (
                SELECT id FROM roles
                WHERE name = 'DELIVERY_PARTNER'
            )
            """, active, partnerId);

        if (updated == 0) {
            throw notFound("Delivery partner not found");
        }

        return active
            ? "Delivery partner activated successfully"
            : "Delivery partner deactivated successfully";
    }

    // ==========================================
    // 5. CUSTOMER ASSIGNMENT
    // ==========================================

    @Transactional
    public String assignCustomer(Long partnerId, Long customerId) {

        Long partnerExists = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE u.id = ?
            AND r.name = 'DELIVERY_PARTNER'
            AND u.is_active = TRUE
            """, Long.class, partnerId);

        if (partnerExists == null || partnerExists == 0) {
            throw notFound("Active delivery partner not found");
        }

        Long customerExists = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM customers WHERE id = ?
            """, Long.class, customerId);

        if (customerExists == null || customerExists == 0) {
            throw notFound("Customer not found");
        }

        Long alreadyAssigned = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM delivery_partner_customers
            WHERE customer_id = ?
            """, Long.class, customerId);

        if (alreadyAssigned != null && alreadyAssigned > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Customer is already assigned to a delivery partner"
            );
        }

        jdbcTemplate.update("""
            INSERT INTO delivery_partner_customers (
                delivery_partner_user_id,
                customer_id
            )
            VALUES (?, ?)
            """, partnerId, customerId);

        return "Customer assigned successfully";
    }

    public List<Map<String, Object>> getAssignedCustomers(Long partnerId) {

        return jdbcTemplate.queryForList("""
            SELECT
                c.id AS customerId,
                u.full_name AS fullName,
                u.email,
                u.phone,
                dpc.assigned_at AS assignedAt
            FROM delivery_partner_customers dpc
            JOIN customers c ON c.id = dpc.customer_id
            JOIN users u ON u.id = c.user_id
            WHERE dpc.delivery_partner_user_id = ?
            ORDER BY dpc.assigned_at DESC
            """, partnerId);
    }

    @Transactional
    public String removeCustomerAssignment(
            Long partnerId,
            Long customerId) {

        int deleted = jdbcTemplate.update("""
            DELETE FROM delivery_partner_customers
            WHERE delivery_partner_user_id = ?
            AND customer_id = ?
            """, partnerId, customerId);

        if (deleted == 0) {
            throw notFound("Customer assignment not found");
        }

        return "Customer assignment removed successfully";
    }

    // ==========================================
    // 6. ORDER MANAGEMENT
    // ==========================================

    public List<Map<String, Object>> getOrders(String status) {

        if (status == null || status.isBlank()) {

            return jdbcTemplate.queryForList("""
                SELECT
                    o.id AS orderId,
                    o.order_number AS orderNumber,
                    o.customer_id AS customerId,
                    cu.full_name AS customerName,
                    cu.phone AS customerPhone,
                    o.delivery_partner_user_id AS deliveryPartnerId,
                    dp.full_name AS deliveryPartnerName,
                    o.total_amount AS totalAmount,
                    o.status,
                    o.delivery_address AS deliveryAddress,
                    o.created_at AS orderedAt
                FROM orders o
                JOIN customers c ON c.id = o.customer_id
                JOIN users cu ON cu.id = c.user_id
                LEFT JOIN users dp
                    ON dp.id = o.delivery_partner_user_id
                ORDER BY o.created_at DESC
                """);
        }

        return jdbcTemplate.queryForList("""
            SELECT
                o.id AS orderId,
                o.order_number AS orderNumber,
                o.customer_id AS customerId,
                cu.full_name AS customerName,
                cu.phone AS customerPhone,
                o.delivery_partner_user_id AS deliveryPartnerId,
                dp.full_name AS deliveryPartnerName,
                o.total_amount AS totalAmount,
                o.status,
                o.delivery_address AS deliveryAddress,
                o.created_at AS orderedAt
            FROM orders o
            JOIN customers c ON c.id = o.customer_id
            JOIN users cu ON cu.id = c.user_id
            LEFT JOIN users dp
                ON dp.id = o.delivery_partner_user_id
            WHERE o.status = ?
            ORDER BY o.created_at DESC
            """, status.trim().toUpperCase(Locale.ROOT));
    }

    @Transactional
    public String updateOrderStatus(Long orderId, String status) {

        if (status == null ||
                !ORDER_STATUSES.contains(
                    status.trim().toUpperCase(Locale.ROOT)
                )) {

            throw badRequest("Invalid order status");
        }

        int updated = jdbcTemplate.update("""
            UPDATE orders
            SET status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """,
            status.trim().toUpperCase(Locale.ROOT),
            orderId
        );

        if (updated == 0) {
            throw notFound("Order not found");
        }

        return "Order status updated successfully";
    }

    @Transactional
    public String assignOrderToPartner(Long orderId, Long partnerId) {

        Long partnerExists = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM users u
            JOIN roles r ON u.role_id = r.id
            WHERE u.id = ?
            AND r.name = 'DELIVERY_PARTNER'
            AND u.is_active = TRUE
            """, Long.class, partnerId);

        if (partnerExists == null || partnerExists == 0) {
            throw notFound("Active delivery partner not found");
        }

        int updated = jdbcTemplate.update("""
            UPDATE orders
            SET delivery_partner_user_id = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            AND status NOT IN ('DELIVERED', 'CANCELLED')
            """, partnerId, orderId);

        if (updated == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Order not found or cannot be assigned"
            );
        }

        return "Delivery partner assigned to order successfully";
    }

    // ==========================================
    // COMMON ERROR HANDLING
    // ==========================================

    private ResponseStatusException notFound(String message) {

        return new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            message
        );
    }

    private ResponseStatusException badRequest(String message) {

        return new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            message
        );
    }
}