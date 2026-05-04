package com.ecommerce.order.service;

import com.ecommerce.order.dto.*;
import com.ecommerce.order.exception.OrderException;
import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderCreate;
import com.ecommerce.order.model.OrderList;
import com.ecommerce.order.model.OrderQuery;
import com.ecommerce.order.repository.OrderPersistenceAdapter;
import com.ecommerce.order.repository.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderPersistenceAdapter orderPersistenceAdapter;
    private final OrdersRepository orderRepository;


    public Mono<ApiResponse<OrderList>> getOrders(OrderQuery query) {

        Mono<List<Order>> dataMono = orderPersistenceAdapter.findByCriteria(query).collectList();

        Mono<Integer> countMono = orderPersistenceAdapter.countByCriteria(query);

        return Mono.zip(dataMono, countMono)
                .map(tuple -> {
                    List<Order> orders = tuple.getT1(); // Risultato di dataMono
                    Integer totalElements = tuple.getT2();    // Risultato di countMono
                    OrderList pList = OrderList.builder().results(orders).total(totalElements).limit(query.getLimit()).offset(query.getOffset()).build();
                    return ApiResponse.<OrderList>builder()
                            .success(true)
                            .message("Ricerca completata")
                            .data(pList)
                            .timestamp(System.currentTimeMillis())
                            .build();
                });
    }

    public Mono<ApiResponse<Order>> createOrder(OrderCreate order) {

        return orderRepository.insert(new Order(order))
                .map(orderOut -> ApiResponse.<Order>builder().data(orderOut).
                        success(true).timestamp(System.currentTimeMillis())
                        .message("prodotto creato con id"+orderOut.getId()).build())
                .onErrorResume(e -> Mono.just(ApiResponse.<Order>builder()
                        .success(false)
                        .message("Errore durante la creazione: " + e.getMessage())
                        .build()));

    }


    public Mono<ApiResponse<Order>> getOrderById(String id) {
        return orderRepository.findById(id)
                .map(order -> ApiResponse.<Order>builder()
                        .data(order)
                        .success(true)
                        .message("Prodotto trovato")
                        .timestamp(System.currentTimeMillis())
                        .build())
                .switchIfEmpty(Mono.error(new OrderException.NotFoundException("Non trovato")))
                .onErrorResume(e -> Mono.just(ApiResponse.<Order>builder()
                        .success(false)
                        .message("Errore: " + e.getMessage())
                        .build()));
    }


    public Mono<ApiResponse<Order>> deleteOrder(String id, String requesterId, boolean isAdmin) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderException.NotFoundException("Prodotto non trovato con id: " + id)))
                .flatMap(order -> {
                    if (!isAdmin && !order.getOrdOwner().equals(requesterId)) {
                        return Mono.error(new OrderException.ForbiddenException("Non autorizzato: solo il proprietario o un admin può eliminare questo prodotto"));
                    }
                    return orderRepository.delete(order)
                            .thenReturn(ApiResponse.<Order>builder()
                                    .data(null)
                                    .success(true)
                                    .message("Prodotto eliminato con id: " + id)
                                    .timestamp(System.currentTimeMillis())
                                    .build());
                });
    }

    public Mono<ApiResponse<Order>> updateOrder(String id, Order orderDetails, String requesterId, boolean isAdmin) {
        return orderRepository.findById(id)
                .switchIfEmpty(Mono.error(new OrderException.NotFoundException("Prodotto non trovato con id: " + id)))
                .flatMap(existingOrder -> {
                    if (!isAdmin && !existingOrder.getOrdOwner().equals(requesterId)) {
                        return Mono.error(new OrderException.ForbiddenException("Non autorizzato: solo il proprietario o un admin può modificare questo prodotto"));
                    }
                    if (orderDetails.getDate() != null) {
                        existingOrder.setDate(orderDetails.getDate());
                    }

                    if (orderDetails.getOrdOwner() != null) {
                        existingOrder.setOrdOwner(orderDetails.getOrdOwner());
                    }

                    if (orderDetails.getTotal() != null) {
                        existingOrder.setTotal(orderDetails.getTotal());
                    }

                    if (orderDetails.getStatus() != null) {
                        existingOrder.setStatus(orderDetails.getStatus());
                    }

                    if (orderDetails.getItems() != null) {
                        existingOrder.setItems(orderDetails.getItems());
                    }
                    return orderRepository.save(existingOrder);
                })
                .map(updatedOrder -> ApiResponse.<Order>builder()
                        .success(true)
                        .data(updatedOrder)
                        .message("Prodotto aggiornato con successo")
                        .timestamp(System.currentTimeMillis())
                        .build());
    }


    public Mono<ApiResponse<Order>> patchOrder(String id, Order orderDetails) {
        return orderRepository.findById(id)
                .flatMap(existingOrder -> {
                    // Aggiorni i campi dell'oggetto esistente

                    return orderRepository.save(existingOrder);
                })
                .map(updatedOrder -> ApiResponse.<Order>builder()
                        .success(true)
                        .data(updatedOrder)
                        .message("Prodotto aggiornato con successo")
                        .timestamp(System.currentTimeMillis())
                        .build())
                .switchIfEmpty(Mono.just(ApiResponse.<Order>builder()
                        .success(false)
                        .message("Prodotto non trovato con id: " + id)
                        .build()));
    }



}
