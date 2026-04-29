package com.ecommerce.catalog.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreate {

    @NotBlank
    private String name;
    private String category;
    private String description;
    @Positive
    private Float price;
    @PositiveOrZero
    private Integer stockQuantity;
    private String imageUrl;
}