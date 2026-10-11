package demo.shop.order;

import demo.shop.api.OrdersApi;
import demo.shop.api.model.HistoryEntryDto;
import demo.shop.api.model.NewOrderDto;
import demo.shop.api.model.OrderDto;
import demo.shop.history.OrderHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class OrderController implements OrdersApi {
    private final OrderService orders;
    private final OrderHistoryService history;

    public OrderController(OrderService orders, OrderHistoryService history) {
        this.orders = orders;
        this.history = history;
    }

    @Override
    public ResponseEntity<OrderDto> placeOrder(NewOrderDto newOrder) {
        List<OrderItem> items = newOrder.getLines().stream()
            .map(line -> new OrderItem(line.getProductId(), line.getQuantity()))
            .collect(Collectors.toList());
        Order placed = orders.place(newOrder.getCustomerId(), items, newOrder.getPromoCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderMapper.toDto(placed));
    }

    @Override
    public ResponseEntity<OrderDto> getOrder(Long id) {
        return ResponseEntity.ok(OrderMapper.toDto(orders.get(id)));
    }

    @Override
    public ResponseEntity<List<OrderDto>> listOrders(Long customerId) {
        return ResponseEntity.ok(orders.findByCustomer(customerId).stream()
            .map(OrderMapper::toDto)
            .collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<List<HistoryEntryDto>> getOrderHistory(Long id) {
        orders.get(id);
        return ResponseEntity.ok(history.forOrder(id).stream()
            .map(entry -> new HistoryEntryDto()
                .action(entry.getAction())
                .details(entry.getDetails())
                .at(entry.getAt().atOffset(ZoneOffset.UTC)))
            .collect(Collectors.toList()));
    }
}
