package dev.kakrizky.lightwind.events;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * In-process event bus backed by CDI events.
 *
 * <p>Provides synchronous and asynchronous event publishing. Events are fired
 * with the {@link LightEventListener} qualifier so that observers can filter
 * on Lightwind-specific events.</p>
 */
@ApplicationScoped
public class EventBus {

    private static final Logger LOG = Logger.getLogger(EventBus.class);

    @Inject
    @LightEventListener
    Event<LightEvent> eventEmitter;

    /**
     * Fires a CDI event synchronously.
     *
     * @param event the event to publish
     */
    public void publish(LightEvent event) {
        LOG.debugf("Publishing event synchronously: type=%s, sourceType=%s, sourceId=%s",
                event.getEventType(), event.getSourceType(), event.getSourceId());
        eventEmitter.fire(event);
    }

    /**
     * Fires a CDI event asynchronously.
     *
     * @param event the event to publish
     */
    public void publishAsync(LightEvent event) {
        LOG.debugf("Publishing event asynchronously: type=%s, sourceType=%s, sourceId=%s",
                event.getEventType(), event.getSourceType(), event.getSourceId());
        eventEmitter.fireAsync(event);
    }

    /**
     * Type-safe variant that selects a CDI event subtype before firing.
     *
     * <p>Useful when observers are qualified on a specific {@link LightEvent} subclass.</p>
     *
     * @param event     the event to publish
     * @param eventClass the concrete event class for CDI subtype selection
     * @param <T>       the event type
     */
    public <T extends LightEvent> void publish(T event, Class<T> eventClass) {
        LOG.debugf("Publishing typed event synchronously: type=%s, class=%s",
                event.getEventType(), eventClass.getSimpleName());
        eventEmitter.select(eventClass).fire(event);
    }
}
