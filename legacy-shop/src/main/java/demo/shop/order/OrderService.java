package demo.shop.order;

import com.corp.commons.util.CorpLogger;
import demo.shop.product.Product;
import demo.shop.product.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
public class OrderService {
    private static final CorpLogger log = CorpLogger.getLogger(OrderService.class);

    private final ProductRepository products;
    private final PricingService pricing;
    private final DeliveryClient delivery;

    public OrderService(ProductRepository products, PricingService pricing, DeliveryClient delivery) {
        this.products = products;
        this.pricing = pricing;
        this.delivery = delivery;
    }

    public OrderConfirmation place(OrderRequest request) {
        Product product = products.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No product " + request.getProductId()));
        BigDecimal items = pricing.total(product.getPrice(), request.getQuantity());
        BigDecimal shipping = delivery.quote(request.getProductId(), request.getQuantity());
        log.info("Order placed: {} x {}", request.getQuantity(), product.getName());
        return new OrderConfirmation(product.getName(), request.getQuantity(), items, shipping);
    }
}
