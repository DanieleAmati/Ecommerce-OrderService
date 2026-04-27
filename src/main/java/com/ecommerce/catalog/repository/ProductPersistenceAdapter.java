package com.ecommerce.catalog.repository;

import com.ecommerce.catalog.model.Product;
import com.ecommerce.catalog.model.ProductQuery;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements ProductsRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public Flux<Product> findByCriteria(ProductQuery query) {
        Query mongoQuery = new Query();

        if (query.getProdName() != null) {
            mongoQuery.addCriteria(Criteria.where("name").is(query.getProdName()));
        }
        if (query.getProdCategory() != null) {
            mongoQuery.addCriteria(Criteria.where("category").is(query.getProdCategory()));
        }

        mongoQuery.skip(query.getOffset()).limit(query.getLimit());

        return mongoTemplate.find(mongoQuery, Product.class);
    }

    public Mono<Integer> countByCriteria(ProductQuery query) {
        Query mongoQuery = new Query();
        if (query.getProdName() != null) {
            mongoQuery.addCriteria(Criteria.where("name").is(query.getProdName()));
        }
        if (query.getProdCategory() != null) {
            mongoQuery.addCriteria(Criteria.where("category").is(query.getProdCategory()));
        }
        return  mongoTemplate.count(mongoQuery, Product.class).map(result -> result.intValue());
    }

    @Override
    public <S extends Product> Mono<S> insert(S entity) {
        return null;
    }

    @Override
    public <S extends Product> Flux<S> insert(Iterable<S> entities) {
        return null;
    }

    @Override
    public <S extends Product> Flux<S> insert(Publisher<S> entities) {
        return null;
    }

    @Override
    public <S extends Product> Mono<S> findOne(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Product> Flux<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Product> Flux<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends Product> Mono<Long> count(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Product> Mono<Boolean> exists(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Product, R, P extends Publisher<R>> P findBy(Example<S> example, Function<FluentQuery.ReactiveFluentQuery<S>, P> queryFunction) {
        return null;
    }

    @Override
    public <S extends Product> Mono<S> save(S entity) {
        return null;
    }

    @Override
    public <S extends Product> Flux<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public <S extends Product> Flux<S> saveAll(Publisher<S> entityStream) {
        return null;
    }

    @Override
    public Mono<Product> findById(String s) {
        return null;
    }

    @Override
    public Mono<Product> findById(Publisher<String> id) {
        return null;
    }

    @Override
    public Mono<Boolean> existsById(String s) {
        return null;
    }

    @Override
    public Mono<Boolean> existsById(Publisher<String> id) {
        return null;
    }

    @Override
    public Flux<Product> findAll() {
        return null;
    }

    @Override
    public Flux<Product> findAllById(Iterable<String> strings) {
        return null;
    }

    @Override
    public Flux<Product> findAllById(Publisher<String> idStream) {
        return null;
    }

    @Override
    public Mono<Long> count() {
        return null;
    }

    @Override
    public Mono<Void> deleteById(String s) {
        return null;
    }

    @Override
    public Mono<Void> deleteById(Publisher<String> id) {
        return null;
    }

    @Override
    public Mono<Void> delete(Product entity) {
        return null;
    }

    @Override
    public Mono<Void> deleteAllById(Iterable<? extends String> strings) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends Product> entities) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends Product> entityStream) {
        return null;
    }

    @Override
    public Mono<Void> deleteAll() {
        return null;
    }

    @Override
    public Flux<Product> findAll(Sort sort) {
        return null;
    }
}
