package com.afet.monitoring.domain.port;

import com.afet.monitoring.domain.model.Earthquake;
import java.util.List;

/**
 * Outbound Port — the ONE interface the application talks to for seismic feeds,
 * regardless of the source. Kandilli, AFAD and USGS each expose a totally different
 * raw API; an Adapter per source implements this port and normalises everything into
 * the domain {@link Earthquake} model.
 *
 * <p>The application depends only on this abstraction (DIP), so it has no idea how
 * many sources exist or what their wire formats look like.
 */
public interface SeismicFeedPort {

    /** Human-readable source name, used as the earthquake's {@code source} field. */
    String sourceName();

    /** Recent events from this source, already normalised to the domain model (unscored). */
    List<Earthquake> fetchRecent();
}
