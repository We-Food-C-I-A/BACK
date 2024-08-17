package com.wefood.back.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * class: CreateProductRequestDto.
 *
 * @author JBum
 * @version 2024/08/18
 */
@Getter
@Setter
public class CreateProductRequestDto {

    @NotNull(message = "물건의 이름을 입력해주세요.")
    private String name;

    @NotNull(message = "물건의 소개를 입력해주세요.")
    private String detail;

    @NotNull(message = "가격을 적어주세요")
    @Min(value = 10, message = "10이상이어야 합니다.")
    private Integer price;

    @NotNull(message = "카테고리가 선택되지 않았습니다.")
    @Min(value = 1, message = "1이상이어야 합니다.")
    private Long categoryId;

    @Size(max = 5, message = "태그 ID는 최대 5개까지 입력할 수 있습니다.")
    private List<String> tags;

}
