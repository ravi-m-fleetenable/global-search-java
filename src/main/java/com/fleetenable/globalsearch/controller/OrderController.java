package com.fleetenable.globalsearch.controller;

import com.fleetenable.globalsearch.model.Order;
import com.fleetenable.globalsearch.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Order REST Controller
 *
 * Handles partitioned orders table with partition-aware queries
 */
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Order management APIs")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderRepository orderRepository;

    @GetMapping
    @Operation(summary = "Get recent orders", description = "Retrieve recent orders (default: last 6 months)")
    public ResponseEntity<List<Order>> getRecentOrders(
        @Parameter(description = "Months to look back")
        @RequestParam(defaultValue = "6") int months
    ) {
        Instant since = Instant.now().minus(months * 30L, ChronoUnit.DAYS);
        List<Order> orders = orderRepository.findRecentOrders(since);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order by ID")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        // Note: For partitioned tables, we need composite key
        // This is a simplified version
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by order number", description = "Retrieve order by order number")
    public ResponseEntity<Order> getOrderByNumber(@PathVariable String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get orders by account", description = "Retrieve all orders for an account")
    public ResponseEntity<List<Order>> getOrdersByAccount(@PathVariable Long accountId) {
        List<Order> orders = orderRepository.findByAccountId(accountId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieve orders by status")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderRepository.findByStatusAndDeletedAtIsNull(orderStatus);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/delayed")
    @Operation(summary = "Get delayed orders", description = "Retrieve orders past estimated delivery date")
    public ResponseEntity<List<Order>> getDelayedOrders() {
        List<Order> orders = orderRepository.findDelayedOrders(LocalDate.now());
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/search")
    @Operation(summary = "Search orders with fuzzy matching", description = "Search orders using pg_trgm (recent only)")
    public ResponseEntity<List<Order>> searchOrders(
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Months to look back (default: 6 for partition pruning)")
        @RequestParam(defaultValue = "6") int months,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Maximum results")
        @RequestParam(defaultValue = "50") int limit
    ) {
        Instant startDate = Instant.now().minus(months * 30L, ChronoUnit.DAYS);
        List<Order> orders = orderRepository.searchFuzzyRecent(q, startDate, threshold, limit);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/search/all")
    @Operation(summary = "Search all orders", description = "Search across all partitions (slower)")
    public ResponseEntity<List<Order>> searchAllOrders(
        @Parameter(description = "Search query", required = true)
        @RequestParam String q,

        @Parameter(description = "Similarity threshold (0.0-1.0)")
        @RequestParam(defaultValue = "0.3") double threshold,

        @Parameter(description = "Maximum results")
        @RequestParam(defaultValue = "50") int limit
    ) {
        log.warn("Searching across all order partitions - this may be slow");
        List<Order> orders = orderRepository.searchFuzzyAll(q, threshold, limit);
        return ResponseEntity.ok(orders);
    }

    @PostMapping
    @Operation(summary = "Create order", description = "Create a new order")
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "Update an existing order")
    public ResponseEntity<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        // Simplified - needs composite key handling
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "Soft delete an order")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        // Simplified - needs composite key handling
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
