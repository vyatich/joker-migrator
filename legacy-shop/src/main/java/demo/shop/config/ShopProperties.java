package demo.shop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConstructorBinding
@ConfigurationProperties("shop")
public class ShopProperties {
    /** How many lines one order may have. */
    private final int maxLinesPerOrder;
    /** How long a placed order waits for shipping before it expires and its stock is released. */
    private final Duration orderExpiry;

    public ShopProperties(@DefaultValue("20") int maxLinesPerOrder, @DefaultValue("P2D") Duration orderExpiry) {
        this.maxLinesPerOrder = maxLinesPerOrder;
        this.orderExpiry = orderExpiry;
    }

    public int getMaxLinesPerOrder() {
        return maxLinesPerOrder;
    }

    public Duration getOrderExpiry() {
        return orderExpiry;
    }
}
