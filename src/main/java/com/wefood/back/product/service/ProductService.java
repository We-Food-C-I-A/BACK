package com.wefood.back.product.service;

import com.wefood.back.farm.dto.FarmInfoResponse;
import com.wefood.back.farm.entity.Farm;
import com.wefood.back.farm.repository.FarmRepository;
import com.wefood.back.global.image.repository.FarmImageRepository;
import com.wefood.back.global.image.repository.ProductImageRepository;
import com.wefood.back.product.dto.CreateProductRequest;
import com.wefood.back.product.dto.ProductDetailResponse;
import com.wefood.back.global.image.dto.ImageDetailResponse;
import com.wefood.back.product.dto.ProductResponse;
import com.wefood.back.product.entity.Category;
import com.wefood.back.product.entity.Product;
import com.wefood.back.product.entity.ProductCategory;
import com.wefood.back.product.entity.ProductTag;
import com.wefood.back.product.entity.Tag;
import com.wefood.back.product.exception.CategoryNotFoundException;
import com.wefood.back.product.exception.ProductNotFoundException;
import com.wefood.back.product.repository.CategoryRepository;
import com.wefood.back.product.repository.ProductCategoryRepository;
import com.wefood.back.product.repository.ProductRepository;
import com.wefood.back.product.repository.ProductTagRepository;
import com.wefood.back.product.repository.TagRepository;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@jakarta.transaction.Transactional
public class ProductService {

    @Value("${wefood.config.image.address}")
    private String imgRoute;
    @Value("${wefood.config.image.productURL}")
    private String productURL;
    @Value("${wefood.config.image.farmURL}")
    private String farmURL;
    private final String slash = "/";

    private final ProductRepository productRepository;
    private final ProductTagRepository productTagRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;

    private final FarmImageRepository farmImageRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final FarmRepository farmRepository;

    private final TagRepository tagRepository;

    public ProductService(ProductRepository productRepository,
        ProductTagRepository productTagRepository, ProductImageRepository productImageRepository, CategoryRepository categoryRepository, FarmImageRepository farmImageRepository,
        ProductCategoryRepository productCategoryRepository, FarmRepository farmRepository,
        TagRepository tagRepository) {
        this.productRepository = productRepository;
        this.productTagRepository = productTagRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.farmImageRepository = farmImageRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.farmRepository = farmRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * 단일 상품을 조회하는 메서드
     *
     * @param id productId
     * @return ProductDetailResponseDto
     */
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetail(Long id) {
        Optional<ProductDetailResponse> productDetailResponse = productRepository.findProductDetailByProductId(id);
        if (productDetailResponse.isEmpty()) {
            throw new ProductNotFoundException();
        }
        List<ImageDetailResponse> productImages = productImageRepository.findImageByProductId(id);
        for (ImageDetailResponse image : productImages) {
            image.setName(imgRoute + productURL + slash + id + slash + image.getImg());
        }
        productDetailResponse.get().setProductImg(productImages);

        FarmInfoResponse farm = productRepository.findFarmById(id);
        List<ImageDetailResponse> farmImages = farmImageRepository.findByPk_FarmId(farm.getFarm().getId());
        for (ImageDetailResponse image : farmImages) {
            image.setName(imgRoute + farmURL + slash + farm.getFarm().getId() + slash + image.getImg());
        }
        productDetailResponse.get().setFarmImg(farmImages);
        productDetailResponse.get().setFarmId(farm.getFarm().getId());
        productDetailResponse.get().setFarmName(farm.getFarm().getName());
        productDetailResponse.get().setFarmDetail(farm.getFarm().getDetail());

        return productDetailResponse.get();
    }

    /**
     * 최근 상품 4개 조회
     *
     * @return product list
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts() {
        List<ProductResponse> products = productRepository.findTop4ByOrderByIdDesc();
        for (ProductResponse product : products) {
            product.setImg(imgRoute + productURL + slash + product.getId() + slash + product.getImg());
        }
        return products;
    }

    /**
     * 카테고리별 상품 조회
     *
     * @param categoryId category
     * @param pageable   Page
     * @return Page 별 product
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException();
        }

        Page<ProductResponse> products = productRepository.findProductByCategoryId(categoryId, pageable);
        for (ProductResponse product : products) {
            product.setImg(imgRoute + productURL + slash + product.getId() + slash + product.getImg());
        }
        return products;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductBySearch(String search, Pageable pageable) {
        Page<ProductResponse> products = productRepository.findByNameLike(search, pageable);
        for (ProductResponse product : products) {
            product.setImg(imgRoute + productURL + slash + product.getId() + slash + product.getImg());
        }

        return products;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductByTag(String search, Pageable pageable) {
        Page<ProductResponse> products = productRepository.findByTag(search.replace("#", ""), pageable);
        for (ProductResponse product : products) {
            product.setImg(imgRoute + productURL + slash + product.getId() + slash + product.getImg());
        }

        return products;
    }

    public void create(Long farmId, CreateProductRequest createProductRequest) {
        Farm farm = farmRepository.findById(farmId).orElseThrow(()->new IllegalArgumentException(farmId+"의 농가는 존재하지않습니다"));
        Category category = categoryRepository.findById(createProductRequest.getCategoryId()).orElseThrow(()->new IllegalArgumentException(createProductRequest.getCategoryId()+"의 카테고리는 존재하지않습니다"));
        Product product = productRepository.save(Product.builder().name(createProductRequest.getName()).price(createProductRequest.getPrice()).detail(
            createProductRequest.getDetail()).isStatus(true).farm(farm).build());
        productCategoryRepository.save(ProductCategory.builder().pk(ProductCategory.Pk.builder().productId(
                product.getId()).categoryId(
                category.getId())
            .build()).build());
        List<Tag> tags = createProductRequest.getTags().stream()
            .map(tag -> tagRepository.findByName(tag)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(tag).build())))
            .collect(Collectors.toList());

        List<ProductTag> productTags = tags.stream()
            .map(tag -> ProductTag.builder()
                .pk(ProductTag.Pk.builder()
                    .tagId(tag.getId())          // Tag 객체에서 ID를 가져옴
                    .productId(product.getId())  // Product 객체에서 ID를 가져옴
                    .build())
                .build())
            .collect(Collectors.toList());

        productTagRepository.saveAll(productTags);
    }
}
