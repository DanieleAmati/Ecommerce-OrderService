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
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("orders")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreate {

    @NotNull
    private LocalDateTime date;

    @NotNull
    @Positive
    private Float total;

    @NotBlank
    private String name;

    @NotNull
    private OrderStatus status;

    @NotNull
    @Valid
    private Order.Address shippingAddress;

    @NotEmpty
    @Valid
    private List<Order.OrderItem> items;

    private String ordOwner;

}
