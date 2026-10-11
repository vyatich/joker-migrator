package demo.shop.product;

import demo.shop.api.ProductsApi;
import demo.shop.api.model.NewProductDto;
import demo.shop.api.model.ProductDto;
import demo.shop.review.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductController implements ProductsApi {
    private final ProductService products;
    private final ReviewService reviews;

    public ProductController(ProductService products, ReviewService reviews) {
        this.products = products;
        this.reviews = reviews;
    }

    @Override
    public ResponseEntity<List<ProductDto>> listProducts(Long categoryId) {
        List<Product> found = categoryId == null ? products.findAll() : products.findByCategory(categoryId);
        return ResponseEntity.ok(found.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<ProductDto> getProduct(Long id) {
        return ResponseEntity.ok(toDto(products.get(id)));
    }

    @Override
    public ResponseEntity<ProductDto> createProduct(NewProductDto newProduct) {
        Product created = products.create(newProduct.getName(), newProduct.getPrice(), newProduct.getCategoryId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    private ProductDto toDto(Product product) {
        return ProductMapper.toDto(product, reviews.averageRating(product.getId()).orElse(null));
    }
}
