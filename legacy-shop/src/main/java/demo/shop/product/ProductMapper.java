package demo.shop.product;

import demo.shop.api.model.ProductDto;

final class ProductMapper {
    private ProductMapper() {
    }

    static ProductDto toDto(Product product) {
        return new ProductDto()
            .id(product.getId())
            .name(product.getName())
            .price(product.getPrice());
    }
}
