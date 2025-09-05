package com.example.ordersystem.ordering.dto;

import com.example.ordersystem.ordering.domain.OrderDetail;
import com.example.ordersystem.ordering.domain.OrderStatus;
import com.example.ordersystem.ordering.domain.Ordering;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderListResDto {      //[{id:1, memberEmail:test@naver.com, orderStatus:ORDERED, orderDetails:[{},{}]}, {}...]
    private Long id;
    private String memberEmail;
    private OrderStatus orderStatus = OrderStatus.ORDERED;
    private List<OrderDetailResDto> orderDetails;   //for 화면 설계

    public static OrderListResDto fromEntity(Ordering ordering) {
        List<OrderDetailResDto> orderDetailResDtoList = new ArrayList<>();
        for(OrderDetail orderDetail : ordering.getOrderDetailList()) {
            orderDetailResDtoList.add(OrderDetailResDto.fromEntity(orderDetail));
        }
        OrderListResDto dto = OrderListResDto.builder()
                .id(ordering.getId())
                .memberEmail(ordering.getMember().getEmail())
                .orderStatus(ordering.getOrderStatus())
                .orderDetails(orderDetailResDtoList)
                .build();
        return dto;
    }
}
