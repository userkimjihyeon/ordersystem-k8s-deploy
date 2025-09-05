package com.example.ordersystem.product.dto;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreateDto {
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
    private MultipartFile productImage;

//    static 아님! -> dto->entity 변환이므로
    public Product toEntity(Member member) {
        return Product.builder()
                .name(this.name)
                .category(this.category)
                .price(this.price)
                .member(member)
                .stockQuantity(this.stockQuantity)
                .build();
    }
}
