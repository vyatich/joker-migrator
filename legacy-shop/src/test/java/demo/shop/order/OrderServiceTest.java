package demo.shop.order;

import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    ProductRepository products;
    @Mock
    PricingService pricing;
    @Mock
    DeliveryClient delivery;
    @InjectMocks
    OrderService orders;

    @Test
    void placesOrderWithItemsAndDelivery() {
        when(products.findById(7L)).thenReturn(Optional.of(new Product("Laptop", new BigDecimal("1499.00"))));
        when(pricing.total(any(), anyInt())).thenReturn(new BigDecimal("1349.10"));
        when(delivery.quote(anyLong(), anyInt())).thenReturn(new BigDecimal("4.99"));

        OrderRequest request = new OrderRequest();
        request.setProductId(7L);
        request.setQuantity(1);

        OrderConfirmation confirmation = orders.place(request);
        assertThat(confirmation.getTotal()).isEqualByComparingTo("1354.09");
    }

    @Test
    void rejectsUnknownProduct() {
        when(products.findById(anyLong())).thenReturn(Optional.empty());
        OrderRequest request = new OrderRequest();
        request.setProductId(99L);
        request.setQuantity(1);

        assertThatThrownBy(() -> orders.place(request)).isInstanceOf(ResponseStatusException.class);
    }
}
