package com.wefood.back.product.controller;

import com.wefood.back.global.Message;
import com.wefood.back.global.exception.FileUploadException;
import com.wefood.back.global.exception.InvalidRequestException;
import com.wefood.back.global.image.dto.UploadThumbnailRequestDto;
import com.wefood.back.global.image.service.StorageService;
import com.wefood.back.product.dto.ProductDetailResponse;
import com.wefood.back.product.dto.ProductResponse;
import com.wefood.back.global.image.dto.UploadImageRequestDto;
import com.wefood.back.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final StorageService storageService;

    private final ProductService productService;
    private final static String DIR_NAME = "product";
    private final static String successMessage = "상품 조회 성공";

    public ProductController(ProductService productService, StorageService storageService) {
        this.productService = productService;
        this.storageService = storageService;
    }

    /**
     * 최신 상품 조회
     *
     * @return 상품 정보
     */
    @GetMapping
    public ResponseEntity<Message<List<ProductResponse>>> getProducts() {
        Message<List<ProductResponse>> message = new Message<>(200, successMessage, productService.getProducts());
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    /**
     * 카테고리별 상품 조회
     * @param categoryId 카테고리
     * @param pageable Page
     * @return Page별 카테고리
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Message<Page<ProductResponse>>> getProductByCategory(@PathVariable(name = "categoryId") Long categoryId, Pageable pageable) {
        Message<Page<ProductResponse>> message = new Message<>(200, successMessage, productService.getProductByCategory(categoryId, pageable));
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    /**
     * 검색어로 상품 조회(태그/상품 이름)
     *
     * @param search 검색어
     * @param pageable Page
     * @return Page 별 검색 결과
     */
    @GetMapping("/search")
    public ResponseEntity<Message<Page<ProductResponse>>> getProductBySearch(@RequestParam("searchWord") String search, Pageable pageable) {
        Page<ProductResponse> products;
        if (search.strip().startsWith("#")) {
            products = productService.getProductByTag(search, pageable);
        } else {
            products = productService.getProductBySearch(search, pageable);
        }
        Message<Page<ProductResponse>> message = new Message<>(200, successMessage, products);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    /**
     * 상품 상세 조회
     *
     * @param productId 상품 번호
     * @return 상품 상세 정보
     */
    @GetMapping("/{productId}")
    public ResponseEntity<Message<ProductDetailResponse>> getProduct(@PathVariable(name = "productId") Long productId) {
        Message<ProductDetailResponse> message = new Message<>(200, successMessage, productService.getProductDetail(productId));
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadImages(
        @RequestParam("id") Long id,
        @RequestParam("files") List<MultipartFile> files) {

        try {
            storageService.saveImages(id,files, DIR_NAME);
        } catch (IOException e) {
            throw new FileUploadException("An error occurred while uploading files.", e);
        }
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/thumbnail")
    public void uploadThumbnail(@RequestParam("id") Long id,
        @RequestParam("files") MultipartFile files) {
        UploadThumbnailRequestDto requestDto = new UploadThumbnailRequestDto();
        requestDto.setId(id);
        requestDto.setFiles(files);

        try {
            storageService.saveThumbnail(requestDto, DIR_NAME);
        } catch (IOException e) {
            throw new FileUploadException("An error occurred while uploading files.", e);
        }
    }
}
