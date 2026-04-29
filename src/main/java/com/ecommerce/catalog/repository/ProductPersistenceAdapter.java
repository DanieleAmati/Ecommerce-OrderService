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
public class ProductPersistenceAdapter  {

    private final ReactiveMongoTemplate mongoTemplate;

    public Flux<Product>  findByCriteria(ProductQuery query) {
        Query mongoQuery = new Query();

        if (query.getProdName() != null) {
            mongoQuery.addCriteria(Criteria.where("name").regex(query.getProdName(), "i"));
        }
        if (query.getProdCategory() != null) {
            mongoQuery.addCriteria(Criteria.where("category").regex(query.getProdCategory(), "i"));
        }

        Sort sort = Sort.unsorted();
        if (query.getNameOrder() != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(query.getNameOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = sort.and(Sort.by(direction, "name"));
        }
        if (query.getPriceOrder() != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(query.getPriceOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = sort.and(Sort.by(direction, "price"));
        }
        if (sort.isSorted()) {
            mongoQuery.with(sort);
        }

        mongoQuery.skip(query.getOffset()).limit(query.getLimit());

        return mongoTemplate.find(mongoQuery, Product.class);
    }

    public Flux<String> findDistinctCategories() {
        return mongoTemplate.findDistinct("category", Product.class, String.class);
    }

    public Mono<Integer> countByCriteria(ProductQuery query) {
        Query mongoQuery = new Query();
        if (query.getProdName() != null) {
            mongoQuery.addCriteria(Criteria.where("name").regex(query.getProdName(), "i"));
        }
        if (query.getProdCategory() != null) {
            mongoQuery.addCriteria(Criteria.where("category").regex(query.getProdCategory(), "i"));
        }
        return  mongoTemplate.count(mongoQuery, Product.class).map(result -> result.intValue());
    }

}
