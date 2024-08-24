package com.wefood.back.global.image.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * class: UploadImageDto.
 *
 * @author JBumLee
 * @version 2024/08/11
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UploadImageRequestDto {
    @NotNull(message = "파일이 입력되지 않았습니다.")
    private List<MultipartFile> files;

    @NotNull(message = "ID가 null입니다.")
    @Min(value = 1, message = "1이상이어야 합니다.")
    private Long id;
}