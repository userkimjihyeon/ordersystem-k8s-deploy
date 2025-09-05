package com.example.ordersystem.ordering.service;

import com.example.ordersystem.common.service.SseAlarmService;
import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.ordering.domain.OrderDetail;
import com.example.ordersystem.ordering.domain.Ordering;
import com.example.ordersystem.ordering.dto.OrderCreateDto;
import com.example.ordersystem.ordering.dto.OrderListResDto;
import com.example.ordersystem.ordering.repository.OrderDetailRepository;
import com.example.ordersystem.ordering.repository.OrderingRepository;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderingService {
    private final OrderingRepository orderingRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final SseAlarmService sseAlarmService;

    public Long save(List<OrderCreateDto> orderCreateDtoList) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("없는 id입니다."));

        //ORDERING 조립하기 (부모 먼저)
        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        //ORDERINGDETAIL 조립하기 (자식)
        for(OrderCreateDto dto : orderCreateDtoList) {
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("없는 product id입니다."));
//            재고처리(재고(product)가 주문개수(dto)보다 적은경우)
            if(product.getStockQuantity() < dto.getProductCount()) {
//                예외를 강제발생시킴으로써, 모든 임시저장사항들을 rollback처리
                throw new IllegalArgumentException("재고가 부족합니다.");
            }

//            1. 동시에 접근하는 상황에서 udpate값의 정합성이 깨지고 갱신이상이 발생
//            2. spring버전이나 mysql버전에 따라 jpa에서 강제에러(deadlock)를 발생시켜 대부분의 요청실패 발생
            product.updateStockQuantity(dto.getProductCount());   //재고=(재고-주문수량)
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(dto.getProductCount())
                    .ordering(ordering) //자식orderDetail->부모ordering(양방향연결)
                    .build();
//            orderDetailRepository.save(orderDetail);           // cascade 무(repo에 저장)
            ordering.getOrderDetailList().add(orderDetail);      // cascade 유(ordering 객체의 OrderDetailList 컬럼에 저장) //부모->자식(양방향연결)
        }
        orderingRepository.save(ordering);

//        주문성공시 admin 유저에게 알림메시지 전송
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId()); //(리시버,센더,메시지내용)

        return ordering.getId();
    }

    public List<OrderListResDto> findAll() {
//        //원본
//        List<Ordering> orderingList = orderingRepository.findAll();
//        //⭐원본을 dto로 조립
//        List<OrderListResDto> orderListResDtoList = new ArrayList<>();
//        for(Ordering ordering : orderingList) {
//            //⭐한 ordering의 여러 orderDatail => list에서는 굳이 필요없는데 토글바를 열면 디테일나오게 하려고!
//            List<OrderDetail> orderDetailList = ordering.getOrderDetailList();
//            List<OrderDetailResDto> orderDetailResDtoList = new ArrayList<>();
//            for(OrderDetail orderDetail : orderDetailList) {
//                OrderDetailResDto orderDetailResDto = OrderDetailResDto.builder()
//                        .detailId(orderDetail.getId())
//                        .productName(orderDetail.getProduct().getName())
//                        .productCount(orderDetail.getQuantity())
//                        .build();
//                orderDetailResDtoList.add(orderDetailResDto);
//            }
//
//            OrderListResDto dto = OrderListResDto.builder()
//                    .id(ordering.getId())
//                    .memberEmail(ordering.getMember().getEmail())
//                    .orderStatus(ordering.getOrderStatus())
//                    .orderDetails(orderDetailResDtoList)     //⭐
//                    .build();
//            orderListResDtoList.add(dto);
//        }
//
//        return orderListResDtoList;

//        fromEntity + stream 으로 한줄로 처리
        return orderingRepository.findAll().stream().map(o -> OrderListResDto.fromEntity(o)).collect(Collectors.toList());
    }

    public List<OrderListResDto> myOrders() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("없는 멤버 id입니다."));
        return orderingRepository.findAllByMember(member).stream().map(o -> OrderListResDto.fromEntity(o)).collect(Collectors.toList());
    }

    public Ordering cancel(Long id) {
//        Ordering DB에 상태값변경 CANCELED
        Ordering ordering = orderingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("없는 id입니다."));
        ordering.cancelStatus();

        for(OrderDetail orderDetail : ordering.getOrderDetailList()) {
//        rdb재고 업데이트
            orderDetail.getProduct().cancelOrder(orderDetail.getQuantity());
        }
        return ordering;
    }
}
