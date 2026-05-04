package com.ecommerce.order.service;

import com.ecommerce.order.exception.OrderException;
import com.ecommerce.order.model.*;
import com.ecommerce.order.repository.OrderPersistenceAdapter;
import com.ecommerce.order.repository.OrdersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    @Mock
    private OrderPersistenceAdapter orderPersistenceAdapter;
    @Mock
    private OrdersRepository ordersRepository;

    private OrderService orderService;

    private Order sampleOrder;
    private String orderId;
    private final String ownerId    = "user-owner-123";
    private final String otherUserId = "user-other-456";

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderPersistenceAdapter, ordersRepository);
        orderId = UUID.randomUUID().toString();

        sampleOrder = Order.builder()
                .id(orderId)
                .date(LocalDateTime.now())
                .total(120.0f)
                .status(OrderStatus.PENDING)
                .shippingAddress(Order.Address.builder()
                        .fullName("Mario Rossi")
                        .street("Via Roma 1")
                        .city("Milano")
                        .zipCode("20100")
                        .build())
                .items(List.of(Order.OrderItem.builder()
                        .productId("prod-001")
                        .name("Laptop")
                        .quantity(1)
                        .priceUnit(120.0f)
                        .build()))
                .ordOwner(ownerId)
                .build();
    }

    // =========================================================================
    // getOrders
    // =========================================================================

    @Test
    @DisplayName("getOrders: restituisce una lista di ordini con metadati di paginazione")
    void getOrders_returnsOrderList() {
        OrderQuery query = OrderQuery.builder().limit(10).offset(0).build();

        when(orderPersistenceAdapter.findByCriteria(query)).thenReturn(Flux.just(sampleOrder));
        when(orderPersistenceAdapter.countByCriteria(query)).thenReturn(Mono.just(1));

        StepVerifier.create(orderService.getOrders(query))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getResults()).hasSize(1);
                    assertThat(response.getData().getTotal()).isEqualTo(1);
                    assertThat(response.getData().getResults().get(0).getOrdOwner()).isEqualTo(ownerId);
                })
                .verifyComplete();
    }

    // =========================================================================
    // createOrder
    // =========================================================================

    @Test
    @DisplayName("createOrder: salva un ordine con successo e riporta l'id nel messaggio")
    void createOrder_success() {
        OrderCreate oc = OrderCreate.builder()
                .date(sampleOrder.getDate())
                .total(sampleOrder.getTotal())
                .status(sampleOrder.getStatus())
                .shippingAddress(sampleOrder.getShippingAddress())
                .items(sampleOrder.getItems())
                .ordOwner(sampleOrder.getOrdOwner())
                .build();

        when(ordersRepository.insert(any(Order.class))).thenReturn(Mono.just(sampleOrder));

        StepVerifier.create(orderService.createOrder(oc))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).contains(orderId);
                    assertThat(response.getData().getOrdOwner()).isEqualTo(ownerId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("createOrder: errore di persistenza → success=false con messaggio di errore")
    void createOrder_dbError_returnsFailureResponse() {
        when(ordersRepository.insert(any(Order.class)))
                .thenReturn(Mono.error(new RuntimeException("DB Error")));

        OrderCreate oc = OrderCreate.builder()
                .date(sampleOrder.getDate())
                .total(sampleOrder.getTotal())
                .status(sampleOrder.getStatus())
                .shippingAddress(sampleOrder.getShippingAddress())
                .items(sampleOrder.getItems())
                .ordOwner(sampleOrder.getOrdOwner())
                .build();

        StepVerifier.create(orderService.createOrder(oc))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isFalse();
                    assertThat(response.getMessage()).contains("DB Error");
                })
                .verifyComplete();
    }

    // =========================================================================
    // getOrderById
    // =========================================================================

    @Test
    @DisplayName("getOrderById: ordine trovato → success=true con i dati dell'ordine")
    void getOrderById_found() {
        when(ordersRepository.findById(orderId)).thenReturn(Mono.just(sampleOrder));

        StepVerifier.create(orderService.getOrderById(orderId))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getId()).isEqualTo(orderId);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getOrderById: id inesistente → success=false (fallback via onErrorResume)")
    void getOrderById_notFound_returnsFailureResponse() {
        when(ordersRepository.findById("unknown-id")).thenReturn(Mono.empty());

        StepVerifier.create(orderService.getOrderById("unknown-id"))
                .assertNext(response -> assertThat(response.isSuccess()).isFalse())
                .verifyComplete();
    }

    // =========================================================================
    // deleteOrder
    // =========================================================================

    @Test
    @DisplayName("deleteOrder: il proprietario elimina il proprio ordine")
    void deleteOrder_success_byOwner() {
        when(ordersRepository.findById(orderId)).thenReturn(Mono.just(sampleOrder));
        when(ordersRepository.delete(sampleOrder)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.deleteOrder(orderId, ownerId, false))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getMessage()).contains(orderId);
                })
                .verifyComplete();

        verify(ordersRepository).delete(sampleOrder);
    }

    @Test
    @DisplayName("deleteOrder: un admin può eliminare l'ordine di qualsiasi utente")
    void deleteOrder_success_byAdmin() {
        when(ordersRepository.findById(orderId)).thenReturn(Mono.just(sampleOrder));
        when(ordersRepository.delete(sampleOrder)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.deleteOrder(orderId, otherUserId, true))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteOrder: utente non proprietario e non admin → ForbiddenException")
    void deleteOrder_forbidden_whenNotOwnerAndNotAdmin() {
        when(ordersRepository.findById(orderId)).thenReturn(Mono.just(sampleOrder));

        StepVerifier.create(orderService.deleteOrder(orderId, otherUserId, false))
                .expectErrorMatches(e -> e instanceof OrderException.ForbiddenException)
                .verify();

        verify(ordersRepository, never()).delete(any(Order.class));
    }

    @Test
    @DisplayName("deleteOrder: id inesistente → NotFoundException")
    void deleteOrder_notFound() {
        when(ordersRepository.findById(orderId)).thenReturn(Mono.empty());

        StepVerifier.create(orderService.deleteOrder(orderId, ownerId, false))
                .expectErrorMatches(e -> e instanceof OrderException.NotFoundException)
                .verify();

        verify(ordersRepository, never()).delete(any(Order.class));
    }

    // =========================================================================
    // updateOrder
    // =========================================================================

    @Test
    @DisplayName("updateOrder: il proprietario aggiorna total e status del proprio ordine")
    void updateOrder_success_byOwner() {
        Order updateDetails = Order.builder()
                .total(200.0f)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(ordersRepository.findById(orderId)).thenReturn(Mono.just(sampleOrder));
        when(ordersRepository.save(any(Order.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(orderService.updateOrder(orderId, updateDetails, ownerId, false))
                .assertNext(response -> {
                    assertThat(response.isSuccess()).isTrue();
                    assertThat(response.getData().getTotal()).isEqualTo(200.0f);
                    assertThat(response.getData().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("updateOrder: un admin aggiorna l'ordine di un altro utente")
    void updateOrder_success_byAdmin() {
        Order updateDetails = Order.builder()
                .total(300.0f)
                .status(OrderStatus.SHIPPED)
                .build();

        when(ordersRepository.findById(orderId)).thenReturn(Mono.just(sampleOrder));
        when(ordersRepository.save(any(Order.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(orderService.updateOrder(orderId, updateDetails, otherUserId, true))
                .assertNext(response -> assertThat(response.isSuccess()).isTrue())
                .verifyComplete();
    }

    @Test
    @DisplayName("updateOrder: utente non proprietario e non admin → ForbiddenException")
    void updateOrder_forbidden_whenNotOwnerAndNotAdmin() {
        when(ordersRepository.findById(orderId)).thenReturn(Mono.just(sampleOrder));

        StepVerifier.create(orderService.updateOrder(orderId, sampleOrder, otherUserId, false))
                .expectErrorMatches(e -> e instanceof OrderException.ForbiddenException)
                .verify();

        verify(ordersRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOrder: id inesistente → NotFoundException")
    void updateOrder_notFound() {
        when(ordersRepository.findById("invalid-id")).thenReturn(Mono.empty());

        StepVerifier.create(orderService.updateOrder("invalid-id", sampleOrder, ownerId, false))
                .expectErrorMatches(e -> e instanceof OrderException.NotFoundException)
                .verify();
    }
}
