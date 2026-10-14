package demo.shop.product;

import demo.shop.catalog.Category;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Entity
public class Product {
    @Id
    @GeneratedValue
    private Long id;
    @NotBlank
    private String name;
    @Positive
    private BigDecimal price;
    @ManyToOne
    private Category category;

    protected Product() {
    }

    public Product(String name, BigDecimal price) {
        this(name, price, null);
    }

    public Product(String name, BigDecimal price, Category category) {
        this.name = name;
        this.price = price;
        this.category = category;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Long getCategoryId() {
        return category == null ? null : category.getId();
    }
}
