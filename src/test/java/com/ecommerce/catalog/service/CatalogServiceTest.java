package com.ecommerce.catalog.service;

import com.ecommerce.catalog.dto.ApiResponse;
import com.ecommerce.catalog.model.Product;
import com.ecommerce.catalog.model.ProductCreate;
import com.ecommerce.catalog.model.ProductList;
import com.ecommerce.catalog.model.ProductQuery;
import com.ecommerce.catalog.repository.ProductPersistenceAdapter;
import com.ecommerce.catalog.repository.ProductsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private ProductPersistenceAdapter productPersistenceAdapter;
    @Mock
    private ProductsRepository productsRepository;

    private CatalogService catalogService;

    private Product sampleProduct;
    private String productId;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(productPersistenceAdapter, productsRepository);
        productId = UUID.randomUUID().toString();
        sampleProduct = Product.builder()
                .id(productId)
                .name("Laptop Gaming")
                .category("Electronics")
                .price(1200.0f)
                .stockQuantity(10)
                .build();
    }

    @Test
    @DisplayName("getProducts: restituisce una lista di prodotti con metadati")
    void getProducts_returnsProductList() {
        ProductQuery query = new ProductQuery();
        query.setLimit(10);
        query.setOffset(0);

        when(productPersistenceAdapter.findByCriteria(query)).thenReturn(Flux.just(sampleProduct));
        when(productPersistenceAdapter.countByCriteria(query)).thenReturn(Mono.just(1));

        StepVerifier.create(catalogService.getProducts(query))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getResults()).hasSize(1);
                    assertThat(response.getData().getTotal()).isEqualTo(1);
                    assertThat(response.getData().getResults().get(0).getName()).isEqualTo("Laptop Gaming");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createProduct: salva un prodotto con successo")
    void createProduct_success() {

        ProductCreate pc = ProductCreate.builder().category(sampleProduct.getCategory()).stockQuantity(sampleProduct.getStockQuantity()).description(sampleProduct.getDescription()).name(sampleProduct.getName()).imageUrl(sampleProduct.getImageUrl()).build();
        when(productsRepository.insert(any(Product.class))).thenReturn(Mono.just(sampleProduct));

        StepVerifier.create(catalogService.createProduct(pc))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).contains(productId);
                    assertThat(response.getData().getName()).isEqualTo("Laptop Gaming");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteProduct: elimina un prodotto esistente")
    void deleteProduct_success() {
        when(productsRepository.deleteById(productId)).thenReturn(Mono.empty());

        StepVerifier.create(catalogService.deleteProduct(productId))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).contains(productId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProduct: aggiorna tutti i campi di un prodotto esistente")
    void updateProduct_success() {
        Product updateDetails = Product.builder()
                .name("Updated Name")
                .category("New Category")
                .price(999.0f)
                .stockQuantity(5)
                .build();

        when(productsRepository.findById(productId)).thenReturn(Mono.just(sampleProduct));
        when(productsRepository.save(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(catalogService.updateProduct(productId, updateDetails))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getName()).isEqualTo("Updated Name");
                    assertThat(response.getData().getCategory()).isEqualTo("New Category");
                    assertThat(response.getData().getPrice()).isEqualTo(999.0f);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("patchProduct: aggiorna solo i campi forniti (parziale)")
    void patchProduct_partialUpdate() {
        Product patchDetails = new Product();
        patchDetails.setPrice(1500.0f); // Solo il prezzo cambia

        when(productsRepository.findById(productId)).thenReturn(Mono.just(sampleProduct));
        when(productsRepository.save(any(Product.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(catalogService.patchProduct(productId, patchDetails))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getPrice()).isEqualTo(1500.0f);
                    assertThat(response.getData().getName()).isEqualTo("Laptop Gaming"); // Rimasto invariato
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProduct: restituisce success=false se il prodotto non esiste")
    void updateProduct_notFound() {
        when(productsRepository.findById("invalid-id")).thenReturn(Mono.empty());

        StepVerifier.create(catalogService.updateProduct("invalid-id", sampleProduct))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isFalse();
                    assertThat(response.getMessage()).contains("non trovato");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createProduct: gestisce l'errore in caso di fallimento persistenza")
    void createProduct_error() {
        when(productsRepository.insert(any(Product.class)))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));
        ProductCreate pc = ProductCreate.builder().category(sampleProduct.getCategory()).stockQuantity(sampleProduct.getStockQuantity()).description(sampleProduct.getDescription()).name(sampleProduct.getName()).imageUrl(sampleProduct.getImageUrl()).build();
        StepVerifier.create(catalogService.createProduct(pc))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isFalse();
                    assertThat(response.getMessage()).contains("Errore durante la creazione: DB Error"); // Nota: nel tuo codice l'errore del create ha il messaggio dell'eliminazione
                })
                .verifyComplete();
    }
}