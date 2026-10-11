package demo.shop.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {
    @Test
    void appliesDiscount() {
        PricingService pricing = new PricingService(10);
        assertThat(pricing.total(new BigDecimal("9.90"), 3)).isEqualByComparingTo("26.73");
    }

    @Test
    void readsDiscountFromCorpConfig() {
        PricingService pricing = new PricingService();
        assertThat(pricing.total(new BigDecimal("100.00"), 1)).isEqualByComparingTo("90.00");
    }
}
