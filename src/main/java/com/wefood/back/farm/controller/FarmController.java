package com.wefood.back.farm.controller;

import com.wefood.back.global.exception.FileUploadException;
import com.wefood.back.global.exception.InvalidRequestException;
import com.wefood.back.global.image.service.StorageService;
import com.wefood.back.product.dto.CreateProductRequestDto;
import com.wefood.back.product.dto.UploadImageRequestDto;
import com.wefood.back.product.service.ProductService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * class: FarmController.
 *
 * @author JBumLee
 * @version 2024/08/12
 */
@RestController
@RequestMapping("/api/farm")
public class FarmController {
    private final StorageService storageService;
    private final ProductService productService;
    private final static String DIR_NAME = "farm";

    public FarmController(StorageService storageService, ProductService productService) {
        this.storageService = storageService;
        this.productService = productService;
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public void uploadImages(
        @Valid @ModelAttribute UploadImageRequestDto requestDto,
        BindingResult result) {

        if (result.hasErrors()) {
            throw new InvalidRequestException(result);
        }

        try {
            storageService.saveImages(requestDto, DIR_NAME);
        } catch (IOException e) {
            throw new FileUploadException("An error occurred while uploading files.", e);
        }
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/{farmId}/product")
    public void createProduct(
        @PathVariable("farmId") Long farmId,
        @Valid @RequestBody CreateProductRequestDto createProductRequestDto,
        BindingResult result) {

        // farmId 검증
        if (farmId < 1) {
            throw new IllegalArgumentException("farmId는 1 이상이어야 합니다.");
        }

        // 유효성 검사 결과 처리
        if (result.hasErrors()) {
            throw new InvalidRequestException(result);
        }

        // 서비스 호출
        productService.setProduct(farmId, createProductRequestDto);
    }
}
