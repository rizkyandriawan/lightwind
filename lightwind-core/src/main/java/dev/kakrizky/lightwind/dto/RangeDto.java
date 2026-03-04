package dev.kakrizky.lightwind.dto;

public class RangeDto<T extends Comparable<T>> {

    private T min;
    private T max;

    public RangeDto() {}

    public RangeDto(T min, T max) {
        this.min = min;
        this.max = max;
    }

    public static <T extends Comparable<T>> RangeDto<T> of(T min, T max) {
        return new RangeDto<>(min, max);
    }

    public boolean contains(T value) {
        if (value == null) return false;
        if (min != null && value.compareTo(min) < 0) return false;
        if (max != null && value.compareTo(max) > 0) return false;
        return true;
    }

    public boolean isValid() {
        if (min == null || max == null) return true;
        return min.compareTo(max) <= 0;
    }

    public boolean overlaps(RangeDto<T> other) {
        if (other == null) return false;
        if (this.max == null || other.min == null) return true;
        if (this.min == null || other.max == null) return true;
        return this.min.compareTo(other.max) <= 0 && this.max.compareTo(other.min) >= 0;
    }

    public T getMin() { return min; }
    public void setMin(T min) { this.min = min; }

    public T getMax() { return max; }
    public void setMax(T max) { this.max = max; }
}
