package com.ecommerce.catalog.controller;

import com.ecommerce.catalog.dto.ApiResponse;
import com.ecommerce.catalog.model.Product;
import com.ecommerce.catalog.model.ProductCreate;
import com.ecommerce.catalog.model.ProductList;
import com.ecommerce.catalog.model.ProductQuery;
import com.ecommerce.catalog.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 */
@Slf4j
@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/products")
    public Mono<ResponseEntity<ApiResponse<ProductList>>> getProducts(@Valid @RequestParam(value="name", required = false) String prodName, @Valid @RequestParam(value="category", required = false)  String prodCategory, @Valid @RequestParam(value="limit", required = false, defaultValue = "10") Integer limit, @Valid @RequestParam(value="offset", required = false, defaultValue = "0") Integer offset, @RequestParam(value="order", required = false) String order, @RequestParam(value="seller", required = false) String prodOwner) {
        String nameOrder = null;
        String priceOrder = null;
        if (order != null) {
            if (order.startsWith("name_")) {
                nameOrder = order.substring(5);
            } else if (order.startsWith("price_")) {
                priceOrder = order.substring(6);
            }
        }
        ProductQuery productQuery = ProductQuery.builder().prodName(prodName).prodCategory(prodCategory).
                limit(limit).offset(offset).
                nameOrder(nameOrder).priceOrder(priceOrder)
                .prodOwner(prodOwner).build();
        return catalogService.getProducts(productQuery)
                .map(body -> ResponseEntity.ok(body)).onErrorReturn(ResponseEntity.internalServerError().build());
    }


    @GetMapping("/products/categories")
    public Mono<ResponseEntity<ApiResponse<List<String>>>> getCategories() {
        return catalogService.getCategories()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @PostMapping("/products")
    public Mono<ResponseEntity<ApiResponse<Product>>> createProduct(@Valid @RequestBody ProductCreate product) {
        return catalogService.createProduct(product).map(body -> ResponseEntity.ok(body)).onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @DeleteMapping("/products/{id}")
    Mono<ResponseEntity<Object>> deleteProduct(@PathVariable String id) {
        return catalogService.deleteProduct(id)
                .map(body -> ResponseEntity.noContent().build())
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }


    @PutMapping("/products/{id}")
    Mono<ResponseEntity<ApiResponse<Product>>> updateProduct(@PathVariable String id, @Valid @RequestBody Product product) {
        return catalogService.updateProduct(id, product).map(body -> ResponseEntity.ok(body))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }


    @PatchMapping("/products/{id}")
    Mono<ResponseEntity<ApiResponse<Product>>> patchProduct(@PathVariable String id, @Valid @RequestBody Product product) {
        return catalogService.updateProduct(id, product).map(body -> ResponseEntity.ok(body))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

}
