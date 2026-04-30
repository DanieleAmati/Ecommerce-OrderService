package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.model.Product;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
class CatalogControllerIT {

@LocalServerPort
    private int port;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    private WebTestClient client;
    private Product testProduct;

    private static final String OWNER_ID  = "user-owner-123";
    private static final String OTHER_ID   = "user-other-456";
    private static final String JWT_SECRET = "bXlTdXBlclNlY3JldEtleVRoYXRJc0F0TGVhc3QyNTZCaXRz";

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        testProduct = mongoTemplate.insert(
                Product.builder()
                        .name("Laptop Test")
                        .category("Electronics")
                        .price(999.0f)
                        .stockQuantity(5)
                        .prodOwner(OWNER_ID)
                        .build()
        ).block();
    }

    @AfterEach
    void tearDown() {
        mongoTemplate.dropCollection(Product.class).block();
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

    @Test
    @DisplayName("GET /catalog/products senza auth → 401")
    void getProducts_noAuth_returns401() {
        client.get().uri("/catalog/products")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("GET /catalog/products autenticato → 200 con lista prodotti")
    void getProducts_authenticated_returnsList() {
        asUser(OWNER_ID).get().uri("/catalog/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.results[0].name").isEqualTo("Laptop Test");
    }

    @Test
    @DisplayName("GET /catalog/products?name=Laptop filtra correttamente")
    void getProducts_withNameFilter_returnsFilteredList() {
        asUser(OWNER_ID).get().uri("/catalog/products?name=Laptop")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.total").isEqualTo(1);
    }


    @Test
    @DisplayName("GET /catalog/products/categories → 200 con lista categorie")
    void getCategories_authenticated_returnsCategories() {
        asUser(OWNER_ID).get().uri("/catalog/products/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data[0]").isEqualTo("Electronics");
    }

    @Test
    @DisplayName("GET /catalog/products/{id} prodotto esistente → 200")
    void getProductById_found_returns200() {
        asUser(OWNER_ID).get().uri("/catalog/products/" + testProduct.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.name").isEqualTo("Laptop Test");
    }

    @Test
    @DisplayName("GET /catalog/products/{id} prodotto inesistente → 200 success=false")
    void getProductById_notFound_returnsSuccessFalse() {
        // getProductById usa onErrorResume nel service: restituisce 200 con success=false
        asUser(OWNER_ID).get().uri("/catalog/products/id-inesistente-000")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    // =========================================================================
    // POST /catalog/products
    // =========================================================================

    @Test
    @DisplayName("POST /catalog/products body valido → 200 prodotto creato")
    void createProduct_validBody_returns200() {
        Map<String, Object> body = Map.of(
                "name", "Mouse Wireless",
                "category", "Electronics",
                "price", 49.99,
                "stock_quantity", 20,
                "prod_owner", OWNER_ID
        );

        asUser(OWNER_ID).post().uri("/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.name").isEqualTo("Mouse Wireless");
    }

    @Test
    @DisplayName("POST /catalog/products senza name → 400 VALIDATION_ERROR")
    void createProduct_missingName_returns400() {
        Map<String, Object> body = Map.of(
                "price", 49.99,
                "prod_owner", OWNER_ID
        );

        asUser(OWNER_ID).post().uri("/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("DELETE /catalog/products/{id} senza auth → 401")
    void deleteProduct_noAuth_returns401() {
        client.delete().uri("/catalog/products/" + testProduct.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("DELETE /catalog/products/{id} owner → 204")
    void deleteProduct_byOwner_returns204() {
        asUser(OWNER_ID).delete().uri("/catalog/products/" + testProduct.getId())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /catalog/products/{id} admin → 204")
    void deleteProduct_byAdmin_returns204() {
        asAdmin(OTHER_ID).delete().uri("/catalog/products/" + testProduct.getId())
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    @DisplayName("DELETE /catalog/products/{id} non-owner non-admin → 403")
    void deleteProduct_byOtherUser_returns403() {
        asUser(OTHER_ID).delete().uri("/catalog/products/" + testProduct.getId())
                .exchange()
                .expectStatus().isForbidden();
    }

    // =========================================================================
    // PUT /catalog/products/{id}
    // =========================================================================

    @Test
    @DisplayName("PUT /catalog/products/{id} owner → 200 prodotto aggiornato")
    void updateProduct_byOwner_returns200() {
        Map<String, Object> body = Map.of(
                "name", "Laptop Test Updated",
                "category", "Electronics",
                "price", 899.0,
                "stock_quantity", 3,
                "prod_owner", OWNER_ID
        );

        asUser(OWNER_ID).put().uri("/catalog/products/" + testProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.name").isEqualTo("Laptop Test Updated");
    }

    @Test
    @DisplayName("PUT /catalog/products/{id} non-owner non-admin → 403")
    void updateProduct_byOtherUser_returns403() {
        Map<String, Object> body = Map.of(
                "name", "Hacked",
                "category", "Electronics",
                "price", 1.0,
                "stock_quantity", 0,
                "prod_owner", OTHER_ID
        );

        asUser(OTHER_ID).put().uri("/catalog/products/" + testProduct.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isForbidden();
    }
}
