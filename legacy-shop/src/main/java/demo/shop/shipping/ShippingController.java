package demo.shop.shipping;

import demo.shop.api.ShipmentsApi;
import demo.shop.api.model.ShipmentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;

@RestController
@RequiredArgsConstructor
public class ShippingController implements ShipmentsApi {
    private final ShippingService shipping;

    @Override
    public ResponseEntity<ShipmentDto> shipOrder(Long id) {
        Shipment shipment = shipping.ship(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ShipmentDto()
            .orderId(shipment.getOrderId())
            .trackingNumber(shipment.getTrackingNumber())
            .shippedAt(shipment.getShippedAt().atOffset(ZoneOffset.UTC)));
    }
}
