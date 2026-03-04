package dev.kakrizky.lightwind.testapp;

import dev.kakrizky.lightwind.entity.LightEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;

@Entity
@Table(name = "test_items")
public class TestItem extends LightEntity<TestItem, TestItemDto> {

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Integer price;

    @Column
    private String status;

    @Column
    private String category;

    @Override
    protected Class<TestItemDto> getDtoClass() { return TestItemDto.class; }

    @Override
    protected Class<TestItem> getEntityClass() { return TestItem.class; }

    public static List<String> getFilterableFields() {
        return List.of("name", "price", "status", "category", "createdAt");
    }

    public static List<String> getSearchableFields() {
        return List.of("name", "description");
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
