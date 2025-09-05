package com.example.ordersystem.product.controller;

import com.example.ordersystem.common.dto.CommonDto;
import com.example.ordersystem.product.dto.ProductSearchDto;
import com.example.ordersystem.product.dto.ProductCreateDto;
import com.example.ordersystem.product.dto.ProductResDto;
import com.example.ordersystem.product.dto.ProductUpdateDto;
import com.example.ordersystem.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping("/create")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@ModelAttribute ProductCreateDto productCreateDto) {
        Long id = productService.save(productCreateDto);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품등록 완료")
                        .build()
                ,HttpStatus.CREATED);
    }

    @GetMapping("/list")
    public ResponseEntity<?> findAll(Pageable pageable, ProductSearchDto productSearchDto){
        Page<ProductResDto> productResDtos = productService.findAll(pageable, productSearchDto);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(productResDtos)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품목록조회 완료")
                        .build(), HttpStatus.OK);
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        ProductResDto dto = productService.findById(id);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(dto)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품상세조회 완료")
                        .build(),
                HttpStatus.OK);
    }

    @PutMapping("/update/{productId}")
    public ResponseEntity<?> update(@PathVariable Long productId, @ModelAttribute ProductUpdateDto productUpdateDto) {
        Long id = productService.update(productId, productUpdateDto);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(id)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품수정 완료")
                        .build(),
                HttpStatus.OK);
    }
}
