package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.*;
import com.ecommerce.catalog.model.Product;
import com.ecommerce.catalog.model.ProductCreate;
import com.ecommerce.catalog.model.ProductList;
import com.ecommerce.catalog.model.ProductQuery;
import com.ecommerce.catalog.repository.ProductPersistenceAdapter;
import com.ecommerce.catalog.repository.ProductsRepository;
import com.ecommerce.catalog.security.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductPersistenceAdapter productPersistenceAdapter;
    private final ProductsRepository productRepository;


    public Mono<ApiResponse<ProductList>> getProducts(ProductQuery query) {

        Mono<List<Product>> dataMono = productPersistenceAdapter.findByCriteria(query).collectList();

        Mono<Integer> countMono = productPersistenceAdapter.countByCriteria(query);

        return Mono.zip(dataMono, countMono)
                .map(tuple -> {
                    List<Product> products = tuple.getT1(); // Risultato di dataMono
                    Integer totalElements = tuple.getT2();    // Risultato di countMono
                    ProductList pList = ProductList.builder().results(products).total(totalElements).limit(query.getLimit()).offset(query.getOffset()).build();
                    return ApiResponse.<ProductList>builder()
                            .success(true)
                            .message("Ricerca completata")
                            .data(pList)
                            .timestamp(System.currentTimeMillis())
                            .build();
                });
    }



    public Mono<ApiResponse<List<String>>> getCategories() {
        return productPersistenceAdapter.findDistinctCategories()
                .filter(c -> c != null && !c.isBlank())
                .collectList()
                .map(categories -> ApiResponse.<List<String>>builder()
                        .success(true)
                        .message("Categorie disponibili")
                        .data(categories)
                        .timestamp(System.currentTimeMillis())
                        .build());
    }

    public Mono<ApiResponse<Product>> createProduct(ProductCreate product) {

        return productRepository.insert(new Product(product))
                .map(productOut -> ApiResponse.<Product>builder().data(productOut).
                        success(true).timestamp(System.currentTimeMillis())
                        .message("prodotto creato con id"+productOut.getId()).build())
                .onErrorResume(e -> Mono.just(ApiResponse.<Product>builder()
                        .success(false)
                        .message("Errore durante la creazione: " + e.getMessage())
                        .build()));

    }


    public Mono<ApiResponse<Product>> deleteProduct(String id) {
        return productRepository.deleteById(id)
                .thenReturn(ApiResponse.<Product>builder()
                        .data(null)
                        .success(true)
                        .message("Prodotto eliminato con id: " + id)
                        .timestamp(System.currentTimeMillis())
                        .build())
                .onErrorResume(e -> Mono.just(ApiResponse.<Product>builder()
                        .success(false)
                        .message("Errore durante l'eliminazione: " + e.getMessage())
                        .build()));
    }

    public Mono<ApiResponse<Product>> updateProduct(String id, Product productDetails) {
        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    // Aggiorni i campi dell'oggetto esistente
                    existingProduct.setName(productDetails.getName());
                    existingProduct.setCategory(productDetails.getCategory());
                    existingProduct.setPrice(productDetails.getPrice());
                    existingProduct.setStockQuantity(productDetails.getStockQuantity());

                    // Salvi l'oggetto aggiornato
                    return productRepository.save(existingProduct);
                })
                .map(updatedProduct -> ApiResponse.<Product>builder()
                        .success(true)
                        .data(updatedProduct)
                        .message("Prodotto aggiornato con successo")
                        .timestamp(System.currentTimeMillis())
                        .build())
                .switchIfEmpty(Mono.just(ApiResponse.<Product>builder()
                        .success(false)
                        .message("Prodotto non trovato con id: " + id)
                        .build()));
    }


    public Mono<ApiResponse<Product>> patchProduct(String id, Product productDetails) {
        return productRepository.findById(id)
                .flatMap(existingProduct -> {
                    // Aggiorni i campi dell'oggetto esistente
                    if (productDetails.getName() != null && !productDetails.getName().isEmpty()) {
                        existingProduct.setName(productDetails.getName());
                    }
                    if (productDetails.getCategory() != null && !productDetails.getCategory().isEmpty()) {
                        existingProduct.setCategory(productDetails.getCategory());
                    }
                    if (productDetails.getPrice() != null) {
                        existingProduct.setPrice(productDetails.getPrice());
                    }
                    if (productDetails.getStockQuantity() != null) {
                        existingProduct.setStockQuantity(productDetails.getStockQuantity());
                    }
                    return productRepository.save(existingProduct);
                })
                .map(updatedProduct -> ApiResponse.<Product>builder()
                        .success(true)
                        .data(updatedProduct)
                        .message("Prodotto aggiornato con successo")
                        .timestamp(System.currentTimeMillis())
                        .build())
                .switchIfEmpty(Mono.just(ApiResponse.<Product>builder()
                        .success(false)
                        .message("Prodotto non trovato con id: " + id)
                        .build()));
    }



}
