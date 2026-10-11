package demo.shop.report;

import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
public class SalesReport {
    LocalDate from;
    LocalDate to;
    long orders;
    BigDecimal revenue;
    List<ProductSales> topProducts;
}
