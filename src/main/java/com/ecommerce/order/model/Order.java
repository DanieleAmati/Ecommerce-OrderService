package com.ecommerce.order.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("orders")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    private String id;

    @NotNull
    private LocalDateTime date;

    @NotNull
    @Positive
    private Float total;

    @NotNull
    private OrderStatus status;

    @NotNull
    @Valid
    private Address shippingAddress;

    @NotEmpty
    @Valid
    private List<OrderItem> items;

    private String ordOwner;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderItem {
        @NotBlank
        private String productId;

        private String name;

        @Min(1)
        private Integer quantity;

        @NotNull
        @Positive
        private Float priceUnit;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Address {
        private String fullName;

        @NotBlank
        private String street;

        @NotBlank
        private String city;

        @NotBlank
        private String zipCode;

        private String province;
    }


    public Order(OrderCreate oCreate) {
        this.date = oCreate.getDate();
        this.total = oCreate.getTotal();
        this.status = oCreate.getStatus();
        this.ordOwner = oCreate.getOrdOwner();
        this.items = oCreate.getItems();
        this.total  = oCreate.getTotal();
        this.shippingAddress = oCreate.getShippingAddress();
    }
}