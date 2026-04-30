package com.ecommerce.catalog.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    @Id
    private String id;
    @NotBlank
    private String name;
    private String category;
    private String description;
    @Positive
    private Float price;
    @PositiveOrZero
    private Integer stockQuantity;
    private String imageUrl;
    @NotBlank
    private String prodOwner;

public Product(ProductCreate product){
    this.name = product.getName();
    this.category = product.getCategory();
    this.description = product.getDescription();
    this.price = product.getPrice();
    this.stockQuantity = product.getStockQuantity();
    this.imageUrl = product.getImageUrl();
    this.prodOwner = product.getProdOwner();
}

}
