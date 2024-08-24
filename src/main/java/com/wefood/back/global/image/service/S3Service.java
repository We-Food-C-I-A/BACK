package com.wefood.back.global.image.service;

import com.wefood.back.farm.entity.Farm;
import com.wefood.back.farm.repository.FarmRepository;
import com.wefood.back.global.image.dto.UploadThumbnailRequestDto;
import com.wefood.back.global.image.entity.FarmImage;
import com.wefood.back.global.image.entity.Image;
import com.wefood.back.global.image.entity.ProductImage;
import com.wefood.back.global.image.repository.FarmImageRepository;
import com.wefood.back.global.image.repository.ImageRepository;
import com.wefood.back.global.image.repository.ProductImageRepository;
import com.wefood.back.global.type.ImageRootType;
import com.wefood.back.global.image.dto.UploadImageRequestDto;
import com.wefood.back.product.entity.Product;
import com.wefood.back.product.repository.ProductRepository;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * class: S3Service.
 *
 * AWS S3 전용 서비스
 * 사진 업로드를 지원한다.
 *
 * @author JBumLee
 * @version 2024/08/11
 */
@RequiredArgsConstructor
@Transactional
@Slf4j
public class S3Service implements StorageService {
    private final S3Client s3Client;
    private final ProductRepository productRepository;
    private final FarmRepository farmRepository;
    private final ProductImageRepository productImageRepository;
    private final FarmImageRepository farmImageRepository;
    private final ImageRepository imageRepository;
    @Value("${cloud.aws.s3.bucketName}")
    private String bucket;


    @Override
    public void saveImages(Long id, List<MultipartFile> multipartFiles, String dirName) throws IOException {
        List<Image> images = new ArrayList<>();
        ImageRootType rootType= dbCheckRootType(dirName, id);
        for (MultipartFile multipartFile : multipartFiles) {
            File uploadFile = convert(multipartFile)  // 파일 변환할 수 없으면 에러
                .orElseThrow(() -> new IllegalArgumentException("error: MultipartFile -> File convert fail"));
            String imageUrl = upload(uploadFile, dirName, id);
            String extension = getFileExtension(imageUrl);
            images.add(Image.builder().name(imageUrl).extension(extension).build());
        }
        imageRepository.saveAll(images);
        saveImageTypeRepo(rootType,images);
    }

    @Override
    public void saveThumbnail(Long id, MultipartFile multipartFile, String dirName)
        throws IOException {
        ImageRootType rootType= dbCheckRootType(dirName, id);
            File uploadFile = convert(multipartFile)  // 파일 변환할 수 없으면 에러
                .orElseThrow(() -> new IllegalArgumentException("error: MultipartFile -> File convert fail"));
        String imageUrl = upload(uploadFile, dirName, id);
        String extension = getFileExtension(imageUrl);
        Image image = Image.builder().name(imageUrl).extension(extension).build();
        imageRepository.save(image);
        saveThumbnailTypeRepo(rootType,image);
    }

    private void saveThumbnailTypeRepo(ImageRootType rootType, Image image) {
        if (rootType == null || image == null) {
            throw new IllegalArgumentException("Invalid input: rootType or images cannot be null or empty.");
        }
        if (rootType instanceof Farm farm) {
            saveFarmImages(farm, List.of(image),true);
        } else if (rootType instanceof Product product) {
            saveProductImages(product, List.of(image),true);
        } else {
            throw new IllegalArgumentException("Unsupported ImageRootType: " + rootType.getClass().getName());
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Filename is null or empty.");
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("No file extension found in " + fileName);
        }
        return fileName.substring(lastDotIndex + 1);
    }
    private ImageRootType dbCheckRootType(String dir, Long id){
        if(dir.equals("farm")){
            return farmRepository.findById(id).orElseThrow();
        }else if(dir.equals("product")){
            return productRepository.findById(id).orElseThrow();
        }
        throw new IllegalSelectorException();
    }
    private void saveImageTypeRepo(ImageRootType rootType, List<Image> images) {
        if (rootType == null || images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Invalid input: rootType or images cannot be null or empty.");
        }
        if (rootType instanceof Farm farm) {
            saveFarmImages(farm, images,false);
        } else if (rootType instanceof Product product) {
            saveProductImages(product, images,false);
        } else {
            throw new IllegalArgumentException("Unsupported ImageRootType: " + rootType.getClass().getName());
        }
    }

    private void saveFarmImages(Farm farm, List<Image> images, Boolean isThumbnail) {
        List<FarmImage> farmImages = IntStream.range(0, images.size())
            .mapToObj(index -> {
                Image image = images.get(index);
                Integer sequence = (Integer) (index + 1); // 예시로 sequence를 인덱스 기반으로 설정
                return FarmImage.builder()
                    .isThumbnail(isThumbnail)
                    .pk(FarmImage.Pk.builder()
                        .farmId(farm.getId())
                        .imageId(image.getId())
                        .build())
                    .sequence(isThumbnail?0:sequence)  // sequence 값을 설정
                    .build();
            })
            .collect(Collectors.toList());
        farmImageRepository.saveAll(farmImages);
    }

    private void saveProductImages(Product product, List<Image> images, Boolean isThumbnail) {
        List<ProductImage> productImages = IntStream.range(0, images.size())
            .mapToObj(index -> {
                Image image = images.get(index);
                Integer sequence = (Integer) (index + 1); // 예시로 sequence를 인덱스 기반으로 설정
                return ProductImage.builder()
                    .pk(ProductImage.Pk.builder()
                        .productId(product.getId())
                        .imageId(image.getId())
                        .build())
                    .sequence(isThumbnail?0:sequence)  // sequence 값을 설정
                    .isThumbnail(isThumbnail)
                    .build();
            })
            .collect(Collectors.toList());
        productImageRepository.saveAll(productImages);
    }
    // S3로 파일 업로드하기
    private String upload(File uploadFile, String dirName, Long id) {
        // 폴더가 없으면 생성
        createDirIfNotExists(dirName);

        String fileName = UUID.randomUUID() + uploadFile.getName(); // S3에 저장된 파일 이름
        putS3(uploadFile,dirName + "/" + id + "/" +  fileName); // s3로 업로드
        removeNewFile(uploadFile);
        return fileName;
    }

    // 폴더가 존재하지 않으면 폴더 생성
    private void createDirIfNotExists(String dirName) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(dirName + "/")
                .build();

            s3Client.headObject(headObjectRequest);
            log.info("Directory already exists: {}", dirName);
        } catch (NoSuchKeyException e) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(dirName + "/")
                .build();

            s3Client.putObject(putObjectRequest, RequestBody.empty());
            log.info("Directory created: {}", dirName);
        } catch (S3Exception e) {
            log.error("Error occurred while checking or creating directory: {}", e.awsErrorDetails().errorMessage());
            throw e;
        }
    }

    // S3로 업로드
    private String putS3(File uploadFile, String fileName) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(fileName)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(uploadFile));
        URL url = s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(fileName));
        return url.toString();
    }

// 로컬에 저장된 이미지 지우기
    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }

    // 로컬에 파일 업로드 하기
    private Optional<File> convert(MultipartFile file) throws IOException {
        File convertFile = new File("/usr"+ "/" + file.getOriginalFilename());
        if (convertFile.createNewFile()) { // 바로 위에서 지정한 경로에 File이 생성됨 (경로가 잘못되었다면 생성 불가능)
            try (FileOutputStream fos = new FileOutputStream(convertFile)) { // FileOutputStream 데이터를 파일에 바이트 스트림으로 저장하기 위함
                fos.write(file.getBytes());
            }
            return Optional.of(convertFile);
        }
        return Optional.empty();
    }
}
