package com.example.ordersystem.ordering.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderDetailDto {
    private List<Product> details;
    private Long storeId;
    private String payment;

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class Product{
        private Long productId;
        private Integer productCount;
    }
}
