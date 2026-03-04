package dev.kakrizky.lightwind.events;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CDI qualifier annotation for Lightwind event observers.
 *
 * <p>Use this annotation together with {@code @Observes} or {@code @ObservesAsync}
 * to mark a method as a Lightwind event listener:</p>
 *
 * <pre>{@code
 * void onEntityCreated(@Observes @LightEventListener LightEvent event) {
 *     // handle event
 * }
 * }</pre>
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
public @interface LightEventListener {
}
