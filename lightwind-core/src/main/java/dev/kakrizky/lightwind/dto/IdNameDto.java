package dev.kakrizky.lightwind.dto;

import java.util.UUID;

public class IdNameDto {

    private UUID id;
    private String name;

    public IdNameDto() {}

    public IdNameDto(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public static IdNameDto of(UUID id, String name) {
        return new IdNameDto(id, name);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
