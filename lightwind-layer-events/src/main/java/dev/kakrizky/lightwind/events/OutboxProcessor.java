package dev.kakrizky.lightwind.events;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Scheduled processor that polls the outbox table for PENDING events
 * and publishes them via the {@link EventBus}.
 *
 * <p>Runs every 5 seconds by default. Batch size is configurable via
 * {@code lightwind.events.outbox.batch-size} (default: 50).</p>
 */
@ApplicationScoped
public class OutboxProcessor {

    private static final Logger LOG = Logger.getLogger(OutboxProcessor.class);

    @Inject
    OutboxService outboxService;

    @Inject
    EventBus eventBus;

    @ConfigProperty(name = "lightwind.events.outbox.batch-size", defaultValue = "50")
    int batchSize;

    @Scheduled(every = "5s", identity = "lightwind-outbox-processor")
    @Transactional
    void processOutbox() {
        List<OutboxEvent> pending = outboxService.pollPending(batchSize);

        if (pending.isEmpty()) {
            return;
        }

        LOG.debugf("Processing %d pending outbox events", pending.size());

        for (OutboxEvent outboxEvent : pending) {
            try {
                LightEvent event = new LightEvent();
                event.setEventType(outboxEvent.getEventType());
                event.setPayload(outboxEvent.getPayload());

                eventBus.publish(event);
                outboxService.markPublished(outboxEvent.getId());
            } catch (Exception e) {
                LOG.errorf(e, "Failed to process outbox event: id=%s, type=%s",
                        outboxEvent.getId(), outboxEvent.getEventType());
                outboxService.markFailed(outboxEvent.getId(), e.getMessage());
            }
        }
    }
}
