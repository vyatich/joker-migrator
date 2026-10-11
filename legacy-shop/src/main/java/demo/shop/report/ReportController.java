package demo.shop.report;

import demo.shop.api.ReportsApi;
import demo.shop.api.model.ProductSalesDto;
import demo.shop.api.model.SalesReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ReportController implements ReportsApi {
    private final SalesReportService reports;

    @Override
    public ResponseEntity<SalesReportDto> salesReport(LocalDate from, LocalDate to) {
        SalesReport report = reports.report(from, to);
        return ResponseEntity.ok(new SalesReportDto()
            .from(report.getFrom())
            .to(report.getTo())
            .orders(report.getOrders())
            .revenue(report.getRevenue())
            .topProducts(report.getTopProducts().stream()
                .map(sales -> new ProductSalesDto()
                    .productName(sales.getProductName())
                    .quantity(sales.getQuantity())
                    .revenue(sales.getRevenue()))
                .collect(Collectors.toList())));
    }
}
