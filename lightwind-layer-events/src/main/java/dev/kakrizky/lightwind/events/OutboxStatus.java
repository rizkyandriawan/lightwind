package dev.kakrizky.lightwind.events;

/**
 * Status of an outbox event record.
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
