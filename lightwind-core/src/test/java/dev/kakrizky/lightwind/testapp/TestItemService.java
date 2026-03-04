package dev.kakrizky.lightwind.testapp;

import dev.kakrizky.lightwind.crud.LightCrudService;
import dev.kakrizky.lightwind.exception.ValidationError;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TestItemService extends LightCrudService<TestItem, TestItemDto> {

    @Override
    protected Class<TestItem> getEntityClass() { return TestItem.class; }

    @Override
    protected Class<TestItemDto> getDtoClass() { return TestItemDto.class; }

    @Override
    protected List<ValidationError> validateCreate(TestItemDto dto) {
        List<ValidationError> errors = new ArrayList<>();
        if (dto.getName() == null || dto.getName().isBlank()) {
            errors.add(new ValidationError("name", "Name is required"));
        }
        return errors;
    }

    @Override
    protected List<ValidationError> validateUpdate(TestItemDto dto) {
        return validateCreate(dto);
    }
}
