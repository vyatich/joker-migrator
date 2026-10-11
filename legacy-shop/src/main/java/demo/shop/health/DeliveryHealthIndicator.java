package demo.shop.health;

import demo.shop.order.DeliveryClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Shows the delivery integration settings in the health endpoint. The shop works without the
 * delivery service (flat rate), so the indicator stays up.
 */
@Component
public class DeliveryHealthIndicator implements HealthIndicator {
    private final DeliveryClient delivery;

    public DeliveryHealthIndicator(DeliveryClient delivery) {
        this.delivery = delivery;
    }

    @Override
    public Health health() {
        return Health.up()
            .withDetail("timeoutMs", delivery.timeoutMillis())
            .withDetail("fallback", "flat rate")
            .build();
    }
}
