package com.afet.monitoring.infrastructure.feed.afad;

/**
 * Mirrors AFAD's real apiv2 event JSON (the adaptee format). AFAD returns every numeric
 * field as a STRING, so the adapter parses them. Unknown JSON properties (eventID, type,
 * province, …) are ignored by Jackson. {@code date} is an ISO local date-time in Turkey
 * time, interpreted as Europe/Istanbul by the adapter.
 */
public record AfadEventDto(
        String magnitude,
        String depth,
        String latitude,
        String longitude,
        String location,
        String date) {}   // e.g. "2026-06-20T11:30:00" (local TRT, no offset)
