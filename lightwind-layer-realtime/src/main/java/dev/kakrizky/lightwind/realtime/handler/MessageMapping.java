package dev.kakrizky.lightwind.realtime.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a STOMP message handler for a specific destination.
 * <p>
 * Can be used as metadata on {@link MessageHandler} implementations
 * to declare the destination pattern declaratively.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MessageMapping {

    /**
     * The STOMP destination pattern (e.g., "/app/chat.send").
     */
    String value();
}
