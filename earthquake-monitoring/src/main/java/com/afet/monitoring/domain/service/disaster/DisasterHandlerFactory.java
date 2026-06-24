package com.afet.monitoring.domain.service.disaster;

import com.afet.monitoring.domain.exception.UnsupportedDisasterTypeException;
import com.afet.monitoring.domain.model.DisasterType;
import java.util.List;

/**
 * Factory Pattern — centralises the "which handler do I instantiate?" decision so no
 * caller ever has to {@code new EarthquakeDisasterHandler()} itself.
 *
 * <p><b>Why no {@code switch}/{@code if-else}?</b> The textbook factory branches on a
 * type string:
 * <pre>
 *   switch (type) {
 *       case EARTHQUAKE -&gt; new EarthquakeDisasterHandler();
 *       case FLOOD      -&gt; new FloodDisasterHandler();
 *       ...
 *   }
 * </pre>
 * That works, but every new disaster type forces an edit here — a violation of the
 * Open/Closed Principle. Instead this factory is handed the full list of handlers
 * (assembled by {@code DisasterFactoryConfig}) and asks each one whether it
 * {@code supports} the type. Adding WILDFIRE = a new handler class + one bean line;
 * this class never changes. Same self-selecting design as {@code RiskScoringService}.
 */
public class DisasterHandlerFactory {

    private final List<DisasterHandler> handlers;

    public DisasterHandlerFactory(List<DisasterHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    /** Return the handler responsible for {@code type}, or fail if none is registered. */
    public DisasterHandler create(DisasterType type) {
        return handlers.stream()
                .filter(h -> h.supports(type))
                .findFirst()
                .orElseThrow(() -> new UnsupportedDisasterTypeException(
                        type == null ? null : type.name()));
    }
}
