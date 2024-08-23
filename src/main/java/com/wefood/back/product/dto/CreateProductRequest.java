package com.wefood.back.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * class: CreateProductRequest.
 *
 * @author JBumLee
 * @version 2024/08/24
 */

@Getter
@Setter
public class CreateProductRequest {

    @NotNull(message = "Category ID must not be null")
    private Long categoryId;

    @NotNull(message = "Detail must not be null")
    private String detail;

    @NotNull(message = "Name must not be null")
    private String name;

    private List<String> tags;

    @Min(value = 0, message = "Price must be greater than or equal to 0")
    private int price;
}