package demo.shop.shipping;

import com.corp.commons.util.CorpLogger;
import demo.shop.inventory.InventoryService;
import demo.shop.order.Order;
import demo.shop.order.OrderService;
import demo.shop.order.OrderShippedEvent;
import demo.shop.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class ShippingService {
    private static final CorpLogger log = CorpLogger.getLogger(ShippingService.class);

    private final OrderService orders;
    private final InventoryService inventory;
    private final ShipmentRepository shipments;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Ships a paid order: the reserved stock leaves the warehouse and the order gets a tracking number.
     */
    public Shipment ship(long orderId) {
        Order order = orders.getInStatus(orderId, OrderStatus.PAID);
        order.getLines().forEach(line -> inventory.commit(line.getProductId(), line.getQuantity()));
        order.markShipped();
        Shipment shipment = shipments.save(new Shipment(orderId, trackingNumber(orderId), Instant.now(clock)));
        events.publishEvent(new OrderShippedEvent(orderId, shipment.getTrackingNumber()));
        log.info("Order {} shipped, tracking {}", orderId, shipment.getTrackingNumber());
        return shipment;
    }

    static String trackingNumber(long orderId) {
        return String.format("TRK-%08d", orderId);
    }
}
