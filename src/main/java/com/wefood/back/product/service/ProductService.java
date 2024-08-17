package com.wefood.back.product.service;

import com.wefood.back.farm.entity.Farm;
import com.wefood.back.farm.repository.FarmRepository;
import com.wefood.back.global.image.repository.ProductImageRepository;
import com.wefood.back.product.dto.CreateProductRequestDto;
import com.wefood.back.product.dto.ProductDetailResponse;
import com.wefood.back.product.dto.ProductImageDetailResponse;
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

import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final String imgRoute = "https://s3.ap-northeast-2.amazonaws.com";
    private static final String productURL = "/product/";
    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    private final ProductRepository productRepository;

    private final ProductImageRepository productImageRepository;

    private final CategoryRepository categoryRepository;

    private final ProductCategoryRepository productCategoryRepository;

    private final FarmRepository farmRepository;

    private final TagRepository tagRepository;

    private final ProductTagRepository productTagRepository;

    public ProductService(ProductRepository productRepository, ProductImageRepository productImageRepository, CategoryRepository categoryRepository,
        ProductCategoryRepository productCategoryRepository, FarmRepository farmRepository,
        TagRepository tagRepository, ProductTagRepository productTagRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.farmRepository = farmRepository;
        this.tagRepository = tagRepository;
        this.productTagRepository = productTagRepository;
    }

    /**
     * 단일 상품을 조회하는 메서드
     *
     * @param id productId
     * @return ProductDetailResponseDto
     */
    public ProductDetailResponse getProductDetail(Long id) {
        Optional<ProductDetailResponse> productDetailResponse = productRepository.findProductDetailByProductId(id);
        if (productDetailResponse.isEmpty()) {
            throw new ProductNotFoundException();
        }
        List<ProductImageDetailResponse> imageByProductId = productImageRepository.findImageByProductId(id);
        for (ProductImageDetailResponse response : imageByProductId) {
            response.setImg(imgRoute + "/" + bucketName + productURL + id + "/" + response.getImg());
        }
        productDetailResponse.get().setImg(imageByProductId);

        return productDetailResponse.get();
    }

    /**
     * 최근 상품 4개 조회
     *
     * @return product list
     */
    public List<ProductResponse> getProducts() {
        List<ProductResponse> products = productRepository.findTop4ByOrderByIdDesc();
        for (ProductResponse product : products) {
            product.setImg(imgRoute + "/" + bucketName + productURL + product.getId() + "/" + product.getImg());
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
    public Page<ProductResponse> getProductByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException();
        }

        Page<ProductResponse> products = productRepository.findProductByCategoryId(categoryId, pageable);
        for (ProductResponse product : products) {
            product.setImg(imgRoute + "/" + bucketName + productURL + product.getId() + "/" + product.getImg());
        }
        return products;
    }

    public Page<ProductResponse> getProductBySearch(String search, Pageable pageable) {
        Page<ProductResponse> products = productRepository.findByNameLike(search, pageable);
        for (ProductResponse product : products) {
            product.setImg(imgRoute + "/" + bucketName + productURL + product.getId() + "/" + product.getImg());
        }

        return products;
    }

    public Page<ProductResponse> getProductByTag(String search, Pageable pageable) {
        Page<ProductResponse> products = productRepository.findByTag(search.replace("#", ""), pageable);
        for (ProductResponse product : products) {
            product.setImg(imgRoute + "/" + bucketName + productURL + product.getId() + "/" + product.getImg());
        }

        return products;
    }

    @Transactional
    public void setProduct(Long farmId,CreateProductRequestDto createProductRequestDto){
        Farm farm = farmRepository.findById(farmId)
            .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + farmId));

        Category category = categoryRepository.findById(createProductRequestDto.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + farmId));

        List<Tag> tags = createProductRequestDto.getTags().stream()
            .map(this::getOrCreateTag)
            .collect(Collectors.toList());

        Product product = Product.builder().farm(farm).name("["+farm.getName()+"] "+createProductRequestDto.getName()).detail(createProductRequestDto.getDetail()).price(
            createProductRequestDto.getPrice()).build();

        Product newProduct = productRepository.save(product);

        ProductCategory productCategory = ProductCategory.builder().pk(ProductCategory.Pk.builder().productId(newProduct.getId()).categoryId(
            category.getId()).build()).build();

        List<ProductTag> productTags = tags.stream()
            .map(tag -> createProductTag(newProduct.getId(), tag.getId()))
            .collect(Collectors.toList());

        productTagRepository.saveAll(productTags);

        productCategoryRepository.save(productCategory);

    }

    private Tag getOrCreateTag(String tagName) {
        // Optional<Tag>를 반환하는 findByName 메서드 호출
        Optional<Tag> optionalTag = tagRepository.findByName(tagName);

        // Optional의 orElseGet 메서드를 사용하여 태그가 없으면 새로 생성
        return optionalTag.orElseGet(() -> {
            Tag newTag = Tag.builder().name(tagName).build();
            return tagRepository.save(newTag);
        });
    }

    private ProductTag createProductTag(Long productId, Long tagId) {
        ProductTag productTag = ProductTag.builder().pk(ProductTag.Pk.builder().productId(productId).tagId(tagId).build()).build();
        return productTag;
    }
}
