package demo.shop.report;

import com.corp.commons.util.CorpLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Writes yesterday's sales into the log every morning; the corporate log pipeline picks it up.
 */
@Component
@RequiredArgsConstructor
public class DailySalesReportJob {
    private static final CorpLogger log = CorpLogger.getLogger(DailySalesReportJob.class);

    private final SalesReportService reports;
    private final Clock clock;

    @Scheduled(cron = "${shop.report.cron:0 0 6 * * *}", zone = "UTC")
    public void reportYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        SalesReport report = reports.report(yesterday, yesterday);
        log.info("Sales {}: {} orders, revenue {}", yesterday, report.getOrders(), report.getRevenue());
    }
}
