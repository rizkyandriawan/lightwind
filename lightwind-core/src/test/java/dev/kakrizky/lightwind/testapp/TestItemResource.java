package dev.kakrizky.lightwind.testapp;

import dev.kakrizky.lightwind.crud.LightCrudResource;
import dev.kakrizky.lightwind.crud.LightCrudService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("/api/test-items")
public class TestItemResource extends LightCrudResource<TestItem, TestItemDto> {

    @Inject
    TestItemService service;

    @Override
    protected LightCrudService<TestItem, TestItemDto> getService() {
        return service;
    }

    @Override
    protected Class<TestItemDto> getDtoClass() {
        return TestItemDto.class;
    }
}
