package demo.shop.report;

import lombok.Value;

import java.math.BigDecimal;

@Value
public class ProductSales {
    String productName;
    long quantity;
    BigDecimal revenue;
}
