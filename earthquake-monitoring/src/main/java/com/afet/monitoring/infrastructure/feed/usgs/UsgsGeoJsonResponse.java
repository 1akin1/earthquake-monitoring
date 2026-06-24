package com.afet.monitoring.infrastructure.feed.usgs;

import java.util.List;

/**
 * Mirrors the shape of the USGS earthquake GeoJSON feed (the "adaptee" format).
 * Note the quirks an adapter must absorb:
 *  - magnitude lives under {@code properties.mag}
 *  - time is a UNIX epoch in milliseconds (UTC)
 *  - coordinates are {@code [longitude, latitude, depthKm]} — LON FIRST.
 */
public record UsgsGeoJsonResponse(List<Feature> features) {

    public record Feature(Properties properties, Geometry geometry) {}

    public record Properties(Double mag, Long time, String place) {}

    /** coordinates = [longitude, latitude, depthKm]. */
    public record Geometry(List<Double> coordinates) {}
}
