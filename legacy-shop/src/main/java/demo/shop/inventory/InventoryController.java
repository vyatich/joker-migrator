package demo.shop.inventory;

import demo.shop.api.InventoryApi;
import demo.shop.api.model.StockDto;
import demo.shop.api.model.StockUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InventoryController implements InventoryApi {
    private final InventoryService inventory;

    @Override
    public ResponseEntity<StockDto> getStock(Long productId) {
        return ResponseEntity.ok(toDto(inventory.get(productId)));
    }

    @Override
    public ResponseEntity<StockDto> setStock(Long productId, StockUpdateDto update) {
        return ResponseEntity.ok(toDto(inventory.set(productId, update.getQuantity())));
    }

    private static StockDto toDto(StockItem item) {
        return new StockDto()
            .productId(item.getProductId())
            .available(item.getAvailable())
            .reserved(item.getReserved());
    }
}
