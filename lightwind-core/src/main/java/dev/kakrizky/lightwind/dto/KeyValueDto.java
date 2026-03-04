package dev.kakrizky.lightwind.dto;

public class KeyValueDto {

    private String key;
    private String value;

    public KeyValueDto() {}

    public KeyValueDto(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static KeyValueDto of(String key, String value) {
        return new KeyValueDto(key, value);
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
