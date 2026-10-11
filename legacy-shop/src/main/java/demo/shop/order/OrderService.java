package demo.shop.order;

import com.corp.commons.util.CorpLogger;
import demo.shop.config.ShopProperties;
import demo.shop.customer.CustomerService;
import demo.shop.inventory.InventoryService;
import demo.shop.product.Product;
import demo.shop.product.ProductService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class OrderService {
    private static final CorpLogger log = CorpLogger.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final CustomerService customers;
    private final ProductService products;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final DeliveryClient delivery;
    private final ApplicationEventPublisher events;
    private final ShopProperties properties;

    public OrderService(OrderRepository orders, CustomerService customers, ProductService products,
                        InventoryService inventory, PricingService pricing, DeliveryClient delivery,
                        ApplicationEventPublisher events, ShopProperties properties) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.inventory = inventory;
        this.pricing = pricing;
        this.delivery = delivery;
        this.events = events;
        this.properties = properties;
    }

    /**
     * Reserves stock for every line, prices the order and asks for a delivery quote. Any failure
     * rolls the whole order back, including the reservations made so far.
     */
    public Order place(long customerId, List<OrderItem> items) {
        customers.get(customerId);
        if (items.size() > properties.getMaxLinesPerOrder()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "An order may have at most " + properties.getMaxLinesPerOrder() + " lines");
        }
        Order order = new Order(customerId, Instant.now());
        int itemCount = 0;
        for (OrderItem item : items) {
            Product product = products.get(item.getProductId());
            inventory.reserve(product.getId(), item.getQuantity());
            BigDecimal amount = pricing.total(product.getPrice(), item.getQuantity());
            order.addLine(product.getId(), product.getName(), item.getQuantity(), amount);
            itemCount += item.getQuantity();
        }
        order.setDelivery(delivery.quote(customerId, itemCount));
        Order saved = orders.save(order);
        events.publishEvent(new OrderPlacedEvent(saved.getId(), customerId, saved.getTotal()));
        log.info("Order {} placed: {} lines, total {}", saved.getId(), items.size(), saved.getTotal());
        return saved;
    }

    @Transactional(readOnly = true)
    public Order get(long id) {
        return orders.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomer(long customerId) {
        return orders.findByCustomerIdOrderByPlacedAtDesc(customerId);
    }

    /**
     * Expires orders that have waited longer than the configured period and gives their stock back.
     *
     * @return how many orders expired
     */
    public int expireStale(Instant now) {
        List<Order> stale = orders.findPlacedBefore(OrderStatus.PLACED, now.minus(properties.getOrderExpiry()));
        for (Order order : stale) {
            order.getLines().forEach(line -> inventory.release(line.getProductId(), line.getQuantity()));
            order.expire();
        }
        return stale.size();
    }
}
