package demo.shop.order;

import demo.shop.api.OrdersApi;
import demo.shop.api.model.NewOrderDto;
import demo.shop.api.model.OrderDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class OrderController implements OrdersApi {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @Override
    public ResponseEntity<OrderDto> placeOrder(NewOrderDto newOrder) {
        List<OrderItem> items = newOrder.getLines().stream()
            .map(line -> new OrderItem(line.getProductId(), line.getQuantity()))
            .collect(Collectors.toList());
        Order placed = orders.place(newOrder.getCustomerId(), items);
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
}
