package com.example.ordersystem.ordering.dto;

import com.example.ordersystem.ordering.domain.OrderDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder    //res는 builder추가해서 조립
public class OrderDetailResDto {
        private Long detailId;
        private String productName;
        private Integer productCount;

        public static OrderDetailResDto fromEntity(OrderDetail orderDetail) {
                OrderDetailResDto orderDetailResDto = OrderDetailResDto.builder()
                        .detailId(orderDetail.getId())
                        .productName(orderDetail.getProduct().getName())
                        .productCount(orderDetail.getQuantity())
                        .build();
                return orderDetailResDto;
        }
}
