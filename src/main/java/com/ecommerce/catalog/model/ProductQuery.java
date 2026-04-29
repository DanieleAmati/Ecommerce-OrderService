package com.ecommerce.catalog.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductQuery {

    private String prodName;
    private String prodCategory;
    private Integer limit;
    private Integer offset;
    private String nameOrder;
    private String priceOrder;
}
