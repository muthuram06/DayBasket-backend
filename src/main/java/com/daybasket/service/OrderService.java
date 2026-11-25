package com.daybasket.service;

import com.daybasket.dto.order.CreateOrderRequest;
import com.daybasket.dto.order.OrderItemRequest;
import com.daybasket.dto.order.OrderItemResponse;
import com.daybasket.dto.order.OrderResponse;
import com.daybasket.dto.order.PaymentVerifyRequest;
import com.daybasket.entity.Customer;
import com.daybasket.entity.Order;
import com.daybasket.entity.OrderItem;
import com.daybasket.entity.Payment;
import com.daybasket.entity.Product;
import com.daybasket.entity.User;
import com.daybasket.repository.CustomerRepository;
import com.daybasket.repository.OrderRepository;
import com.daybasket.repository.PaymentRepository;
import com.daybasket.repository.ProductRepository;
import com.daybasket.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    private final String razorpayKeyId;
    private final String razorpayKeySecret;

    private final RestClient razorpayClient =
        RestClient.create("https://api.razorpay.com/v1");

    public OrderService(
            OrderRepository orderRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            @Value("${app.razorpay.key-id:}") String razorpayKeyId,
            @Value("${app.razorpay.key-secret:}") String razorpayKeySecret) {

        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
    }

    // --------------------------------------------------
    // CREATE ORDER
    // --------------------------------------------------

    @Transactional
    public OrderResponse createOrder(
            String email,
            CreateOrderRequest request) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        Customer customer = customerRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer profile not found"));

        String paymentMethod =
            request.paymentMethod().toUpperCase(Locale.ROOT);

        if (!paymentMethod.equals("COD")
                && !paymentMethod.equals("RAZORPAY")) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid payment method");
        }

        if (paymentMethod.equals("RAZORPAY")
                && (razorpayKeyId.isBlank()
                || razorpayKeySecret.isBlank())) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Razorpay is not configured");
        }

        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setCustomer(customer);
        order.setDeliveryAddress(request.deliveryAddress().trim());
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus("PENDING");
        order.setStatus(
            paymentMethod.equals("COD") ? "PLACED" : "PAYMENT_PENDING"
        );

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.items()) {

            if (itemRequest.quantity() == null
                    || itemRequest.quantity() < 1) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid item quantity");
            }

            Product product = productRepository
                .findById(itemRequest.productId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"));

            if (!Boolean.TRUE.equals(product.getIsActive())) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    product.getName() + " is unavailable");
            }

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < itemRequest.quantity()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock for " + product.getName());
            }

            BigDecimal unitPrice =
                product.getDiscountPrice() != null
                && product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0
                ? product.getDiscountPrice()
                : product.getPrice();

            BigDecimal itemSubtotal =
                unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setProductSku(product.getSku());
            orderItem.setQuantity(itemRequest.quantity());
            orderItem.setUnit(product.getUnit());
            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(itemSubtotal);

            order.addItem(orderItem);
            total = total.add(itemSubtotal);
        }

        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid order amount");
        }

        order.setTotalAmount(total);

        // COD orders are confirmed immediately.
        if (paymentMethod.equals("COD")) {

            reduceStock(order);

            Order savedOrder = orderRepository.save(order);

            return toResponse(savedOrder, null);
        }

        // Create a Razorpay order for online payments.
        RazorpayOrderResult gatewayOrder =
            createRazorpayOrder(total, order.getOrderNumber());

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(total);
        payment.setGatewayOrderId(gatewayOrder.id());
        payment.setPaymentStatus("INITIATED");

        Order savedOrder = orderRepository.save(order);
        paymentRepository.save(payment);

        return toResponse(savedOrder, gatewayOrder.id());
    }

    // --------------------------------------------------
    // RAZORPAY ORDER CREATION
    // --------------------------------------------------

    private RazorpayOrderResult createRazorpayOrder(
            BigDecimal amount,
            String receipt) {

        try {
            long amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

            RazorpayOrderRequest gatewayRequest =
                new RazorpayOrderRequest(
                    amountInPaise,
                    "INR",
                    receipt
                );

            RazorpayOrderResult result = razorpayClient
                .post()
                .uri("/orders")
                .headers(headers ->
                    headers.setBasicAuth(
                        razorpayKeyId,
                        razorpayKeySecret
                    )
                )
                .body(gatewayRequest)
                .retrieve()
                .body(RazorpayOrderResult.class);

            if (result == null || result.id() == null) {
                throw new IllegalStateException(
                    "Razorpay returned an empty order");
            }

            return result;

        } catch (Exception exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Unable to create Razorpay order",
                exception
            );
        }
    }

    // --------------------------------------------------
    // VERIFY RAZORPAY PAYMENT
    // --------------------------------------------------

    @Transactional
    public OrderResponse verifyPayment(
            String email,
            PaymentVerifyRequest request) {

        if (razorpayKeySecret.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Razorpay is not configured");
        }

        Payment payment = paymentRepository
            .findByGatewayOrderId(request.razorpayOrderId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Payment record not found"));

        Order order = payment.getOrder();

        if (!order.getCustomer().getUser().getEmail().equalsIgnoreCase(email)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "This payment does not belong to you");
        }

        if ("SUCCESS".equals(payment.getPaymentStatus())
                && "PAID".equals(order.getPaymentStatus())) {
            return toResponse(order, payment.getGatewayOrderId());
        }

        if (!verifySignature(
                request.razorpayOrderId(),
                request.razorpayPaymentId(),
                request.razorpaySignature())) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Payment signature verification failed");
        }

        // Check stock again before confirming the paid order.
        for (OrderItem item : order.getItems()) {

            Product product = productRepository
                .findById(item.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"));

            if (product.getStockQuantity() == null
                    || product.getStockQuantity() < item.getQuantity()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock to complete the order");
            }
        }

        reduceStock(order);

        payment.setGatewayPaymentId(request.razorpayPaymentId());
        payment.setPaymentSignature(request.razorpaySignature());
        payment.setPaymentStatus("SUCCESS");

        order.setPaymentStatus("PAID");
        order.setStatus("PLACED");

        orderRepository.save(order);
        paymentRepository.save(payment);

        return toResponse(order, payment.getGatewayOrderId());
    }

    private boolean verifySignature(
            String orderId,
            String paymentId,
            String receivedSignature) {

        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] generated = mac.doFinal(
                payload.getBytes(StandardCharsets.UTF_8)
            );

            byte[] received = HexFormat.of().parseHex(receivedSignature);

            return MessageDigest.isEqual(generated, received);

        } catch (Exception exception) {
            return false;
        }
    }

    // --------------------------------------------------
    // STOCK MANAGEMENT
    // --------------------------------------------------

    private void reduceStock(Order order) {

        for (OrderItem item : order.getItems()) {

            Product product = productRepository
                .findById(item.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Product not found"));

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient stock for " + product.getName());
            }

            product.setStockQuantity(
                product.getStockQuantity() - item.getQuantity()
            );

            productRepository.save(product);
        }
    }

    // --------------------------------------------------
    // CUSTOMER ORDER HISTORY
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {

        Customer customer = getCustomer(email);

        return orderRepository
            .findByCustomerIdOrderByCreatedAtDesc(customer.getId())
            .stream()
            .map(order -> toResponse(order, findGatewayOrderId(order)))
            .toList();
    }

    // --------------------------------------------------
    // GET ORDER DETAILS
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(
            String email,
            String orderNumber) {

        Customer customer = getCustomer(email);

        Order order = orderRepository
            .findByOrderNumberAndCustomerId(
                orderNumber,
                customer.getId()
            )
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Order not found"));

        return toResponse(order, findGatewayOrderId(order));
    }

    private Customer getCustomer(String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "User not found"));

        return customerRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Customer profile not found"));
    }


    private String findGatewayOrderId(Order order) {

        if (!"RAZORPAY".equals(order.getPaymentMethod())) {
            return null;
        }

        return paymentRepository.findByOrder_Id(order.getId())
            .map(Payment::getGatewayOrderId)
            .orElse(null);
    }


    // --------------------------------------------------
    // RESPONSE MAPPING
    // --------------------------------------------------

    private OrderResponse toResponse(
            Order order,
            String razorpayOrderId) {

        List<OrderItemResponse> items = order.getItems()
            .stream()
            .map(item -> new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getProductSku(),
                item.getQuantity(),
                item.getUnit(),
                item.getUnitPrice(),
                item.getSubtotal()
            ))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getOrderNumber(),
            order.getTotalAmount(),
            order.getStatus(),
            order.getPaymentMethod(),
            order.getPaymentStatus(),
            order.getDeliveryAddress(),
            order.getCreatedAt(),
            items,
            razorpayOrderId,
            razorpayKeyId
        );
    }

    // --------------------------------------------------
    // ORDER NUMBER GENERATION
    // --------------------------------------------------

    private String generateOrderNumber() {

        String orderNumber;

        do {
            String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase(Locale.ROOT);

            orderNumber = "DB" + randomPart;

        } while (orderRepository.existsByOrderNumber(orderNumber));

        return orderNumber;
    }

    private record RazorpayOrderRequest(
        long amount,
        String currency,
        String receipt
    ) {}

    private record RazorpayOrderResult(
        String id,
        long amount,
        String currency,
        String status
    ) {}
}
