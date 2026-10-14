package demo.shop.catalog;

import com.corp.commons.core.CorpLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private static final CorpLog log = CorpLog.getLogger(CategoryService.class);

    private final CategoryRepository categories;

    public List<Category> findAll() {
        return categories.findAllByOrderByNameAsc();
    }

    public Category get(long id) {
        return categories.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category " + id + " not found"));
    }

    @Transactional
    public Category create(String name) {
        String trimmed = name.trim();
        if (categories.existsByNameIgnoreCase(trimmed)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category " + trimmed + " already exists");
        }
        Category category = categories.save(new Category(trimmed));
        log.info("Created category {} ({})", category.getName(), category.getId());
        return category;
    }
}
