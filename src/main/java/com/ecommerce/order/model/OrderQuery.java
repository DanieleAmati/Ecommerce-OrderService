package com.ecommerce.order.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderQuery {

    private String ordName;
    private String ordStatus;
    private LocalDateTime ordDate;
    private Integer limit;
    private Integer offset;
    private String nameOrder;
    private String dateOrder;
    private String ordOwner;
}
