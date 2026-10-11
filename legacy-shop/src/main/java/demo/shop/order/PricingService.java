package demo.shop.order;

import com.corp.commons.util.CorpContext;
import com.corp.commons.util.CorpLogger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {
    private static final CorpLogger log = CorpLogger.getLogger(PricingService.class);

    private final int discountPercent;

    public PricingService() {
        this(CorpContext.current().getConfig().getInt("shop.discount.percent", 0));
    }

    PricingService(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    /**
     * Promo discount on the items total, rounded to cents.
     */
    public BigDecimal promoDiscount(BigDecimal itemsTotal, int percent) {
        return itemsTotal.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal total(BigDecimal unitPrice, int quantity) {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discount = gross.multiply(BigDecimal.valueOf(discountPercent)).divide(BigDecimal.valueOf(100));
        BigDecimal total = gross.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        log.info("Priced {} x {} with {}% discount: {}", quantity, unitPrice, discountPercent, total);
        return total;
    }
}
