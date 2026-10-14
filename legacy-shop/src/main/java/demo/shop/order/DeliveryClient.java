package demo.shop.order;

import com.corp.commons.core.CorpConfig;
import com.corp.commons.core.CorpContext;
import com.corp.commons.core.CorpHttpClient;
import com.corp.commons.core.CorpLog;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.math.BigDecimal;

/**
 * Asks the delivery service for a shipping quote. Falls back to a flat rate
 * when the service does not answer in time.
 */
@Component
public class DeliveryClient {
    static final BigDecimal FLAT_RATE = new BigDecimal("4.99");

    private static final CorpLog log = CorpLog.getLogger(DeliveryClient.class);

    private final CorpHttpClient http;

    public DeliveryClient() {
        CorpConfig config = CorpContext.current().config();
        this.http = new CorpHttpClient(
            config.getString("shop.delivery.base-url", "http://localhost:8081"),
            Duration.ofMillis(config.getLong("shop.delivery.timeout-ms", 2000)));
    }

    public long timeoutMillis() {
        return http.getTimeout().toMillis();
    }

    public BigDecimal quote(long customerId, int items) {
        try {
            return new BigDecimal(http.get("/quotes?customer=" + customerId + "&items=" + items).trim());
        } catch (IOException | NumberFormatException e) {
            log.warn("Delivery quote failed after {} ms, using flat rate: {}", http.getTimeout().toMillis(), e.getMessage());
            return FLAT_RATE;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return FLAT_RATE;
        }
    }
}
