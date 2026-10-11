package demo.shop.report;

import demo.shop.order.Order;
import demo.shop.order.OrderLine;
import demo.shop.order.OrderRepository;
import demo.shop.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesReportService {
    static final int TOP_PRODUCTS = 5;

    private final OrderRepository orders;

    /**
     * Paid and shipped orders placed between the two dates (UTC, both inclusive).
     */
    public SalesReport report(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from " + from + " is after to " + to);
        }
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusNanos(1);
        List<Order> sold = orders.findByStatusInAndPlacedAtBetween(EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED), start, end);

        BigDecimal revenue = sold.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesReport(from, to, sold.size(), revenue, topProducts(sold));
    }

    private static List<ProductSales> topProducts(List<Order> sold) {
        Map<String, List<OrderLine>> byProduct = sold.stream()
            .flatMap(order -> order.getLines().stream())
            .collect(Collectors.groupingBy(OrderLine::getProductName, TreeMap::new, Collectors.toList()));
        return byProduct.entrySet().stream()
            .map(entry -> new ProductSales(entry.getKey(),
                entry.getValue().stream().mapToLong(OrderLine::getQuantity).sum(),
                entry.getValue().stream().map(OrderLine::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add)))
            .sorted(Comparator.comparingLong(ProductSales::getQuantity).reversed().thenComparing(ProductSales::getProductName))
            .limit(TOP_PRODUCTS)
            .collect(Collectors.toList());
    }
}
