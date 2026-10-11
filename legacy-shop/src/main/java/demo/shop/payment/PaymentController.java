package demo.shop.payment;

import demo.shop.api.PaymentsApi;
import demo.shop.api.model.PaymentDto;
import demo.shop.api.model.PaymentStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentsApi {
    private final PaymentService payments;

    @Override
    public ResponseEntity<PaymentDto> payOrder(Long id) {
        Payment payment = payments.pay(id);
        return ResponseEntity.ok(new PaymentDto()
            .orderId(payment.getOrderId())
            .amount(payment.getAmount())
            .status(PaymentStatusDto.fromValue(payment.getStatus().name()))
            .processedAt(payment.getProcessedAt().atOffset(ZoneOffset.UTC)));
    }
}
