package demo.shop.order;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSenderTest {
    @Test
    void reportsDelivery() throws Exception {
        assertThat(new NotificationSender().send(5L, "order 1 placed").get(1, TimeUnit.SECONDS)).isTrue();
    }
}
