package demo.shop.product;

import demo.shop.catalog.Category;
import demo.shop.catalog.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    private final CategoryService categories;

    public List<Product> findAll() {
        return products.findAll();
    }

    public List<Product> findByCategory(long categoryId) {
        categories.get(categoryId);
        return products.findByCategory_Id(categoryId);
    }

    public Product get(long id) {
        return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Product create(String name, BigDecimal price) {
        return create(name, price, null);
    }

    @Transactional
    public Product create(String name, BigDecimal price, Long categoryId) {
        Category category = categoryId == null ? null : categories.get(categoryId);
        return products.save(new Product(name, price, category));
    }
}
