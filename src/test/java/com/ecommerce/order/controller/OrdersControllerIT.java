package com.ecommerce.order.controller;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class OrdersControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private WebTestClient client;
    private Order testOrder;

    private static final String OWNER_ID  = "user-owner-123";
    private static final String OTHER_ID  = "user-other-456";
    private static final String JWT_SECRET = "bXlTdXBlclNlY3JldEtleVRoYXRJc0F0TGVhc3QyNTZCaXRz";

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        testOrder = mongoTemplate.insert(
                Order.builder()
                        .date(LocalDateTime.now())
                        .total(999.0f)
                        .status(OrderStatus.PENDING)
                        .shippingAddress(Order.Address.builder()
                                .fullName("Test User")
                                .street("Via Test 1")
                                .city("Roma")
                                .zipCode("00100")
                                .build())
                        .items(List.of(Order.OrderItem.builder()
                                .productId("prod-001")
                                .name("Laptop Test")
                                .quantity(1)
                                .priceUnit(999.0f)
                                .build()))
                        .ordOwner(OWNER_ID)
                        .build()
        ).block();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(Order.class).block();
    }

    private String token(String userId, String... roles) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId)
                .claim("roles", List.of(roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(key)
                .compact();
    }

    private WebTestClient asUser(String userId) {
        return client.mutate()
                .defaultHeader("Authorization", "Bearer " + token(userId, "USER"))
                .build();
    }

    private WebTestClient asAdmin(String userId) {
        return client.mutate()
                .defaultHeader("Authorization", "Bearer " + token(userId, "ADMIN"))
                .build();
    }

    // =========================================================================
    // GET /orders
    // =========================================================================

    @Test
    @DisplayName("GET /orders senza auth → 401")
    void getOrders_noAuth_returns401() {
        client.get().uri("/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /orders autenticato → 200 con lista ordini")
    void getOrders_authenticated_returnsList() {
        asUser(OWNER_ID).get().uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.results[0].ord_owner").isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("GET /orders?status=PENDING → 200 lista filtrata per stato")
    void getOrders_withStatusFilter_returnsFilteredList() {
        asUser(OWNER_ID).get().uri("/orders?status=PENDING")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(1);
    }

    // =========================================================================
    // GET /orders/{id}
    // =========================================================================

    @Test
    @DisplayName("GET /orders/{id} ordine esistente → 200 con dati ordine")
    void getOrderById_found_returns200() {
        asUser(OWNER_ID).get().uri("/orders/" + testOrder.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.total").isEqualTo(999.0);
    }

    @Test
    @DisplayName("GET /orders/{id} id inesistente → 200 con success=false")
    void getOrderById_notFound_returnsSuccessFalse() {
        asUser(OWNER_ID).get().uri("/orders/id-inesistente-000")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    // =========================================================================
    // POST /orders
    // =========================================================================

    @Test
    @DisplayName("POST /orders body valido → 200 ordine creato")
    void createOrder_validBody_returns200() {
        Map<String, Object> body = Map.of(
                "date", "2026-01-15T10:00:00",
                "total", 49.99,
                "status", "PENDING",
                "shipping_address", Map.of(
                        "street", "Via Nuova 2",
                        "city", "Torino",
                        "zip_code", "10100"
                ),
                "items", List.of(Map.of(
                        "product_id", "prod-002",
                        "name", "Mouse",
                        "quantity", 1,
                        "price_unit", 49.99
                )),
                "ord_owner", OWNER_ID
        );

        asUser(OWNER_ID).post().uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.ord_owner").isEqualTo(OWNER_ID);
    }

    @Test
    @DisplayName("POST /orders senza items → 400 VALIDATION_ERROR")
    void createOrder_missingItems_returns400() {
        Map<String, Object> body = Map.of(
                "total", 49.99,
                "status", "PENDING"
        );

        asUser(OWNER_ID).post().uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    // =========================================================================
    // DELETE /orders/{id}
    // =========================================================================

    @Test
    @DisplayName("DELETE /orders/{id} senza auth → 401")
    void deleteOrder_noAuth_returns401() {
        client.delete().uri("/orders/" + testOrder.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("DELETE /orders/{id} proprietario → 204")
    void deleteOrder_byOwner_returns204() {
        asUser(OWNER_ID).delete().uri("/orders/" + testOrder.getId())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /orders/{id} admin → 204")
    void deleteOrder_byAdmin_returns204() {
        asAdmin(OTHER_ID).delete().uri("/orders/" + testOrder.getId())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /orders/{id} non-owner non-admin → 403")
    void deleteOrder_byOtherUser_returns403() {
        asUser(OTHER_ID).delete().uri("/orders/" + testOrder.getId())
                .exchange()
                .expectStatus().isForbidden();
    }

    // =========================================================================
    // PUT /orders/{id}
    // =========================================================================

    @Test
    @DisplayName("PUT /orders/{id} proprietario → 200 ordine aggiornato")
    void updateOrder_byOwner_returns200() {
        Map<String, Object> body = Map.of(
                "date", "2026-01-15T10:00:00",
                "total", 850.0,
                "status", "CONFIRMED",
                "shipping_address", Map.of(
                        "street", "Via Aggiornata 5",
                        "city", "Napoli",
                        "zip_code", "80100"
                ),
                "items", List.of(Map.of(
                        "product_id", "prod-001",
                        "name", "Laptop Test",
                        "quantity", 1,
                        "price_unit", 850.0
                )),
                "ord_owner", OWNER_ID
        );

        asUser(OWNER_ID).put().uri("/orders/" + testOrder.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.total").isEqualTo(850.0)
                .jsonPath("$.data.status").isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("PUT /orders/{id} non-owner non-admin → 403")
    void updateOrder_byOtherUser_returns403() {
        Map<String, Object> body = Map.of(
                "date", "2026-01-15T10:00:00",
                "total", 1.0,
                "status", "CANCELLED",
                "shipping_address", Map.of(
                        "street", "Via Falsa 0",
                        "city", "Roma",
                        "zip_code", "00100"
                ),
                "items", List.of(Map.of(
                        "product_id", "prod-001",
                        "name", "Laptop",
                        "quantity", 1,
                        "price_unit", 1.0
                )),
                "ord_owner", OTHER_ID
        );

        asUser(OTHER_ID).put().uri("/orders/" + testOrder.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isForbidden();
    }

    // =========================================================================
    // PATCH /orders/{id}
    // =========================================================================

    @Test
    @DisplayName("PATCH /orders/{id} proprietario → 200 ordine aggiornato parzialmente")
    void patchOrder_byOwner_returns200() {
        Map<String, Object> body = Map.of("total", 500.0);

        asUser(OWNER_ID).patch().uri("/orders/" + testOrder.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true);
    }
}
