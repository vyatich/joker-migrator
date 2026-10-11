package demo.shop.product;

import demo.shop.api.ProductsApi;
import demo.shop.api.model.NewProductDto;
import demo.shop.api.model.ProductDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ProductController implements ProductsApi {
    private final ProductService products;

    public ProductController(ProductService products) {
        this.products = products;
    }

    @Override
    public ResponseEntity<List<ProductDto>> listProducts() {
        return ResponseEntity.ok(products.findAll().stream().map(ProductMapper::toDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<ProductDto> getProduct(Long id) {
        return ResponseEntity.ok(ProductMapper.toDto(products.get(id)));
    }

    @Override
    public ResponseEntity<ProductDto> createProduct(NewProductDto newProduct) {
        Product created = products.create(newProduct.getName(), newProduct.getPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductMapper.toDto(created));
    }
}
