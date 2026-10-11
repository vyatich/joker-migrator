package demo.shop.order;

import com.corp.commons.util.CorpLogger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OrderExpiryJob {
    private static final CorpLogger log = CorpLogger.getLogger(OrderExpiryJob.class);

    private final OrderService orders;

    public OrderExpiryJob(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(initialDelayString = "${shop.order-expiry-check:PT1H}", fixedDelayString = "${shop.order-expiry-check:PT1H}")
    public void expireStaleOrders() {
        int expired = orders.expireStale(Instant.now());
        if (expired > 0) {
            log.info("Expired {} orders and released their stock", expired);
        }
    }
}
