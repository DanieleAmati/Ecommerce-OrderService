package com.ecommerce.order.controller;

import com.ecommerce.order.dto.ApiResponse;
import com.ecommerce.order.exception.OrderException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderCreate;
import com.ecommerce.order.model.OrderList;
import com.ecommerce.order.model.OrderQuery;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 */
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService catalogService;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<OrderList>>> getOrders(@Valid @RequestParam(value="name", required = false) String ordName, @Valid @RequestParam(value="status", required = false)  String ordStatus, @Valid @RequestParam(value="limit", required = false, defaultValue = "10") Integer limit, @Valid @RequestParam(value="offset", required = false, defaultValue = "0") Integer offset, @RequestParam(value="order", required = false) String order, @RequestParam(value="user", required = false) String ordOwner) {
        String nameOrder = null;
        String dateOrder = null;
        if (order != null) {
            if (order.startsWith("name_")) {
                nameOrder = order.substring(5);
            } else if (order.startsWith("date_")) {
                dateOrder = order.substring(5);
            }
        }
        OrderQuery orderQuery = OrderQuery.builder().ordName(ordName).ordStatus(ordStatus).
                limit(limit).offset(offset).
                nameOrder(nameOrder).dateOrder(dateOrder)
                .ordOwner(ordOwner).build();
        return catalogService.getOrders(orderQuery)
                .map(body -> ResponseEntity.ok(body)).doOnError(e -> log.error("Errore nel recupero ordini: {}", e.getMessage()))
                .onErrorResume(e -> {
                    if (e instanceof OrderException.NotFoundException) {
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                    }
                    if (e instanceof IllegalArgumentException) {
                        return Mono.just(ResponseEntity.badRequest().build());
                    }
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }


    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Order>>> getOrderById(@PathVariable String id) {
        return catalogService.getOrderById(id)
                .map(ResponseEntity::ok)
                .doOnError(e -> log.error("Errore nel recupero ordini: {}", e.getMessage()))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<Order>>> createOrder(@Valid @RequestBody OrderCreate order) {
        return catalogService.createOrder(order).map(body -> ResponseEntity.ok(body)).doOnError(e -> log.error("Errore nel recupero ordini: {}", e.getMessage())).onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @DeleteMapping("/{id}")
    Mono<ResponseEntity<Object>> deleteOrder(@PathVariable String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(auth -> {
                    String requesterId = (String) auth.getPrincipal();
                    boolean isAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    return catalogService.deleteOrder(id, requesterId, isAdmin);
                })
                .map(body -> (ResponseEntity<Object>) ResponseEntity.noContent().build());
    }


    @PutMapping("/{id}")
    Mono<ResponseEntity<ApiResponse<Order>>> updateOrder(@PathVariable String id, @Valid @RequestBody Order order) {
            return ReactiveSecurityContextHolder.getContext()
                    .map(ctx -> ctx.getAuthentication())
            .flatMap(auth -> {
        String requesterId = (String) auth.getPrincipal();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return catalogService.updateOrder(id,order, requesterId, isAdmin);
    })
            .map(body -> (ResponseEntity<ApiResponse<Order>>) ResponseEntity.ok(body));
}

    @PatchMapping("/{id}")
    Mono<ResponseEntity<ApiResponse<Order>>> patchOrder(@PathVariable String id, @RequestBody Order order) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .flatMap(auth -> {
                    String requesterId = (String) auth.getPrincipal();
                    boolean isAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    return catalogService.updateOrder(id, order, requesterId, isAdmin);
                })
                .map(body -> (ResponseEntity<ApiResponse<Order>>) ResponseEntity.ok(body));
    }
}
