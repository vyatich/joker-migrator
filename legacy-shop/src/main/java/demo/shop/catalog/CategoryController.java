package demo.shop.catalog;

import demo.shop.api.CategoriesApi;
import demo.shop.api.model.CategoryDto;
import demo.shop.api.model.NewCategoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoriesApi {
    private final CategoryService categories;

    @Override
    public ResponseEntity<List<CategoryDto>> listCategories() {
        return ResponseEntity.ok(categories.findAll().stream().map(CategoryController::toDto).collect(Collectors.toList()));
    }

    @Override
    public ResponseEntity<CategoryDto> createCategory(NewCategoryDto newCategory) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(categories.create(newCategory.getName())));
    }

    static CategoryDto toDto(Category category) {
        return new CategoryDto().id(category.getId()).name(category.getName());
    }
}
