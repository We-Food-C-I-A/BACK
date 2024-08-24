package com.wefood.back.global.image.service;

import com.wefood.back.global.image.dto.UploadImageRequestDto;
import com.wefood.back.global.image.dto.UploadThumbnailRequestDto;
import java.io.IOException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * class: StorageService.
 *
 * ImageService는 이미지를 저장하는 기능을 반드시 지원 할 필요가 있다.
 * 이 떄 한 종류의 Cloud Storage에 종속되지 않게 구현 할 필요가 있다.
 *
 * @author JBumLee
 * @version 2024/08/11
 */
public interface StorageService {
    void saveImages(Long id, List<MultipartFile> multipartFiles, String dirName) throws IOException;

    void saveThumbnail(UploadThumbnailRequestDto uploadThumbnail, String dirName) throws IOException;
}
