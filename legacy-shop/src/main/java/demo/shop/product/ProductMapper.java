package demo.shop.product;

import demo.shop.api.model.ProductDto;

import java.math.BigDecimal;

final class ProductMapper {
    private ProductMapper() {
    }

    static ProductDto toDto(Product product, BigDecimal rating) {
        return new ProductDto()
            .id(product.getId())
            .name(product.getName())
            .price(product.getPrice())
            .categoryId(product.getCategoryId())
            .rating(rating);
    }
}
