package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class OrderPersistenceAdapter  {

    private final ReactiveMongoTemplate mongoTemplate;

    public Flux<Order>  findByCriteria(OrderQuery query) {
        Query mongoQuery = new Query();

        if (query.getOrdName() != null) {
            mongoQuery.addCriteria(Criteria.where("name").regex(query.getOrdName(), "i"));
        }
        if (query.getOrdStatus() != null) {
            mongoQuery.addCriteria(Criteria.where("status").regex(query.getOrdStatus(), "i"));
        }
        if (query.getOrdOwner() != null) {
            mongoQuery.addCriteria(Criteria.where("ordOwner").is(query.getOrdOwner()));
        }

        Sort sort = Sort.unsorted();
        if (query.getNameOrder() != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(query.getNameOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = sort.and(Sort.by(direction, "name"));
        }
        if (query.getDateOrder() != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(query.getDateOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = sort.and(Sort.by(direction, "date"));
        }
        if (sort.isSorted()) {
            mongoQuery.with(sort);
        }

        mongoQuery.skip(query.getOffset()).limit(query.getLimit());

        return mongoTemplate.find(mongoQuery, Order.class);
    }


    public Mono<Integer> countByCriteria(OrderQuery query) {
        Query mongoQuery = new Query();
        if (query.getOrdName() != null) {
            mongoQuery.addCriteria(Criteria.where("name").regex(query.getOrdName(), "i"));
        }
        if (query.getOrdStatus() != null) {
            mongoQuery.addCriteria(Criteria.where("status").regex(query.getOrdStatus(), "i"));
        }
        if (query.getOrdOwner() != null) {
            mongoQuery.addCriteria(Criteria.where("ordOwner").is(query.getOrdOwner()));
        }
        return  mongoTemplate.count(mongoQuery, Order.class).map(result -> result.intValue());
    }

}
