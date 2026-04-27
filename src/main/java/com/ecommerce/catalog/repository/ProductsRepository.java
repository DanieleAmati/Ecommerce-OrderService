package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.model.Product;
import com.ecommerce.catalog.model.ProductQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// è un interfaccia ma viene istanziata da spring data con proxy pattern
public interface ProductsRepository extends ReactiveMongoRepository<Product, String> {



}
