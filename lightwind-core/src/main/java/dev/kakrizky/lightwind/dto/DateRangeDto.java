package dev.kakrizky.lightwind.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DateRangeDto {

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    public DateRangeDto() {}

    public DateRangeDto(LocalDateTime startDate, LocalDateTime endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static DateRangeDto of(LocalDate start, LocalDate end) {
        return new DateRangeDto(
            start != null ? start.atStartOfDay() : null,
            end != null ? end.atTime(23, 59, 59, 999999999) : null
        );
    }

    public static DateRangeDto of(LocalDateTime start, LocalDateTime end) {
        return new DateRangeDto(start, end);
    }

    public boolean contains(LocalDateTime date) {
        if (date == null) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;
        return true;
    }

    public boolean isValid() {
        if (startDate == null || endDate == null) return true;
        return !startDate.isAfter(endDate);
    }

    public long getDurationInDays() {
        if (startDate == null || endDate == null) return 0;
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    public boolean overlaps(DateRangeDto other) {
        if (other == null) return false;
        if (this.endDate == null || other.startDate == null) return true;
        if (this.startDate == null || other.endDate == null) return true;
        return !this.startDate.isAfter(other.endDate) && !this.endDate.isBefore(other.startDate);
    }

    public static DateRangeDto today() {
        LocalDate today = LocalDate.now();
        return of(today, today);
    }

    public static DateRangeDto yesterday() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return of(yesterday, yesterday);
    }

    public static DateRangeDto thisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        return of(startOfWeek, startOfWeek.plusDays(6));
    }

    public static DateRangeDto thisMonth() {
        LocalDate today = LocalDate.now();
        return of(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
    }

    public static DateRangeDto last30Days() {
        LocalDate today = LocalDate.now();
        return of(today.minusDays(30), today);
    }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
}
