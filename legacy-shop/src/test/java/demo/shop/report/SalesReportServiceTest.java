package demo.shop.report;

import demo.shop.order.Order;
import demo.shop.order.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesReportServiceTest {
    @Mock
    OrderRepository orders;

    @Test
    void sumsRevenueAndRanksProductsByQuantity() {
        Order first = order("Coffee", 2, "17.82", "Laptop", 1, "1349.10");
        Order second = order("Coffee", 3, "26.73", "Headphones", 1, "179.10");
        when(orders.findByStatusInAndPlacedAtBetween(anyCollection(), any(), any())).thenReturn(List.of(first, second));

        SalesReport report = new SalesReportService(orders).report(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 11));

        assertThat(report.getOrders()).isEqualTo(2);
        assertThat(report.getRevenue()).isEqualByComparingTo("1572.75");
        assertThat(report.getTopProducts()).extracting(ProductSales::getProductName)
            .containsExactly("Coffee", "Headphones", "Laptop");
        assertThat(report.getTopProducts().getFirst().getQuantity()).isEqualTo(5);
    }

    @Test
    void refusesReversedPeriod() {
        assertThatThrownBy(() -> new SalesReportService(orders).report(LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 1)))
            .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(orders);
    }

    private static Order order(String firstName, int firstQuantity, String firstAmount,
                               String secondName, int secondQuantity, String secondAmount) {
        Order order = new Order(5L, Instant.parse("2026-10-05T10:00:00Z"));
        order.addLine(1L, firstName, firstQuantity, new BigDecimal(firstAmount));
        order.addLine(2L, secondName, secondQuantity, new BigDecimal(secondAmount));
        order.markPaid();
        return order;
    }
}
