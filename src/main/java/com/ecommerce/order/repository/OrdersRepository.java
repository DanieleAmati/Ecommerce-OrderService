package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

// è un interfaccia ma viene istanziata da spring data con proxy pattern
public interface OrdersRepository extends ReactiveMongoRepository<Order, String> {



}
