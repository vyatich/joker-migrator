package demo.shop.inventory;

import com.corp.commons.core.CorpLog;
import demo.shop.product.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class InventoryService {
    private static final CorpLog log = CorpLog.getLogger(InventoryService.class);

    private final StockRepository stock;
    private final ProductRepository products;

    public InventoryService(StockRepository stock, ProductRepository products) {
        this.stock = stock;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public StockItem get(long productId) {
        return stock.findById(productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No stock for product " + productId));
    }

    public StockItem set(long productId, int available) {
        if (!products.existsById(productId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + productId + " not found");
        }
        StockItem item = stock.findById(productId).orElseGet(() -> new StockItem(productId, 0));
        item.setAvailable(available);
        log.info("Stock of product {} set to {}", productId, available);
        return stock.save(item);
    }

    public void reserve(long productId, int quantity) {
        get(productId).reserve(quantity);
    }

    public void commit(long productId, int quantity) {
        stock.findById(productId).ifPresent(item -> item.commit(quantity));
    }

    public void release(long productId, int quantity) {
        stock.findById(productId).ifPresent(item -> item.release(quantity));
    }
}
