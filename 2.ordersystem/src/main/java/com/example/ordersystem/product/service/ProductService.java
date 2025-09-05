package com.example.ordersystem.product.service;

import com.example.ordersystem.member.domain.Member;
import com.example.ordersystem.member.repository.MemberRepository;
import com.example.ordersystem.product.domain.Product;
import com.example.ordersystem.product.dto.ProductCreateDto;
import com.example.ordersystem.product.dto.ProductResDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.dto.ProductUpdateDto;
import com.example.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;

    public Long save(ProductCreateDto productCreateDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(()->new EntityNotFoundException("없는 id입니다."));
        Product product = productRepository.save(productCreateDto.toEntity(member));

        if(productCreateDto.getProductImage() != null) {
//        image명 설정
            String fileName = "product-" + product.getId() + "-productImage-" + productCreateDto.getProductImage().getOriginalFilename();

//        저장 객체 구성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(productCreateDto.getProductImage().getContentType())
                    .build();

//        이미지를 업로드(byte형태로)
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productCreateDto.getProductImage().getBytes()));
            } catch (Exception e) {
//            checked를 unchecked로 바꿔 전체 rollback되도록 예외처리
                throw new IllegalArgumentException("이미지 업로드 실패");
            }

//        이미지 삭제시
//            s3Client.deleteObject(a->a.bucket(버킷명).key(파일명));

//        이미지 url추출
            String imgUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();

            product.updateImageUrl(imgUrl);
        }

        return product.getId();
    }

    public Page<ProductResDto> findAll(Pageable pageable, ProductSearchDto productSearchDto) {
        //        페이지처리 findAll호출
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//                Root : 엔티티의 속성을 접근하기 위한 객체, CriteriaBuilder : 쿼리를 생성하기 위한 객체
                List<Predicate> predicateList = new ArrayList<>();
//                동적
                if (productSearchDto.getCategory() != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("category"), productSearchDto.getCategory()));
                }
                if (productSearchDto.getProductName() != null) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + productSearchDto.getProductName() + "%"));
                }
                Predicate[] predicateArr = new Predicate[predicateList.size()];
                for (int i = 0; i < predicateList.size(); i++) {
                    predicateArr[i] = predicateList.get(i);
                }
//                위의 검색 조건들을 하나(한줄)의 Predicate객체로 만들어서 return
                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };
        Page<Product> productList = productRepository.findAll(specification, pageable);

        return productList.map(p -> ProductResDto.fromEntity(p));    //list객체가 아니라 Page객체이므로 간소해짐

    }

    public ProductResDto findById(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("상품정보없음"));
        return ProductResDto.fromEntity(product);
    }

    public Long update(Long productId, ProductUpdateDto productUpdateDto) {
        Product product = productRepository.findById(productId).orElseThrow(()->new EntityNotFoundException("없는 상품입니다."));
        product.updateProduct(productUpdateDto);

        if(productUpdateDto.getProductImage() != null && !productUpdateDto.getProductImage().isEmpty()) {

//            기존이미지 삭제 : 파일명으로 삭제
            String imageUrl = product.getImagePath();
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/")+1);
            s3Client.deleteObject(a->a.bucket(bucket).key(fileName));

//            신규이미지 등록
            String newFileName = "product-" + product.getId() + "-productImage-" + productUpdateDto.getProductImage().getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(newFileName)
                    .contentType(productUpdateDto.getProductImage().getContentType())
                    .build();

//        이미지를 업로드(byte형태로)
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productUpdateDto.getProductImage().getBytes()));
            } catch (Exception e) {
                throw new IllegalArgumentException("이미지 업로드 실패");
            }

//        이미지 url추출
            String newImageUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImageUrl(newImageUrl);
        } else {
//            s3에서 이미지 삭제 후 url 갱신
            product.updateImageUrl(null);
        }

        return product.getId();
    }
}

