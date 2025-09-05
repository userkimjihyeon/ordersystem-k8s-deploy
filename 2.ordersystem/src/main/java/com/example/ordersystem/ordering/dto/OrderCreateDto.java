package com.example.ordersystem.ordering.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderCreateDto {   //[{"productId":1, "productCount":3}, {"productId":2, "productCount":4}]
    private Long productId;
    private Integer productCount;

    /* 아래형태로 입력받는 방법
    {"details":[{"productId":1, "productCount":3}, {"productId":2, "productCount":4}],
    "storeId":1,
    "payment":"kakao"}

    private List<OrderCreateDto.Product> details;
    private Long storeId;
    private String payment;
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class Product{
        private Long productId;
        private Integer productCount;
    }
    */
}
