package demo.shop.product;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    void seed() {
        if (repository.count() == 0) {
            repository.save(new Product("Coffee", new BigDecimal("9.90")));
            repository.save(new Product("Laptop", new BigDecimal("1499.00")));
        }
    }

    @GetMapping
    public List<Product> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Product one(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product, HttpServletRequest request) {
        Product saved = repository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).header("X-Path", request.getRequestURI()).body(saved);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handle(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatus())
            .body(e.getReason() == null ? e.getStatus().getReasonPhrase() : e.getReason());
    }
}
