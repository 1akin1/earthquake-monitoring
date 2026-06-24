import { useEffect, useRef } from 'react';
import { Map as MapLibreMap, Popup, NavigationControl } from 'maplibre-gl';
import type { StyleSpecification, GeoJSONSource } from 'maplibre-gl';
import type { EarthquakeResponse } from '../api/types';
import { magnitudeColor, riskMeta } from '../lib/risk';
import { fmtMag, fmtDateTime, fmtCoord } from '../lib/format';
import 'maplibre-gl/dist/maplibre-gl.css';

const SOURCE_ID = 'eq';
const LAYER_ID = 'eq-circles';

// CartoDB Voyager raster basemap — a real tiled basemap, light and low-saturation
// so the risk-colored circles stay the loudest thing on screen. No API key needed.
const STYLE: StyleSpecification = {
  version: 8,
  sources: {
    basemap: {
      type: 'raster',
      tiles: [
        'https://a.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
        'https://b.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
        'https://c.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
        'https://d.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
      ],
      tileSize: 256,
      attribution: '&copy; OpenStreetMap &copy; CARTO',
    },
  },
  layers: [{ id: 'basemap', type: 'raster', source: 'basemap' }],
};

// Marker radius grows with magnitude so the strongest events read first.
function radiusFor(mag: number): number {
  return 5 + Math.max(0, mag) * 2.6;
}

function colorFor(eq: EarthquakeResponse): string {
  const meta = riskMeta(eq.riskLevel);
  return meta ? meta.color : magnitudeColor(eq.magnitude);
}

// Each circle is a GeoJSON point; styling is data-driven from these properties.
function toFeatureCollection(
  earthquakes: EarthquakeResponse[],
  selectedId: number | null | undefined,
): GeoJSON.FeatureCollection<GeoJSON.Point> {
  return {
    type: 'FeatureCollection',
    features: earthquakes.map((eq) => ({
      type: 'Feature',
      geometry: { type: 'Point', coordinates: [eq.longitude, eq.latitude] },
      properties: {
        id: eq.id,
        color: colorFor(eq),
        r: radiusFor(eq.magnitude),
        selected: eq.id === selectedId,
        // Pre-formatted strings for the popup (kept on the feature to avoid a lookup).
        mag: fmtMag(eq.magnitude),
        coord: fmtCoord(eq.latitude, eq.longitude),
        depth: `${eq.depthKm.toFixed(1)} km derinlik`,
        time: fmtDateTime(eq.occurredAt),
        source: eq.source,
      },
    })),
  };
}

function popupHtml(p: Record<string, unknown>): string {
  const color = String(p.color ?? '#333');
  const esc = (v: unknown) =>
    String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  return `
    <div class="map-popup">
      <div class="map-popup__mag" style="color:${esc(color)}">M${esc(p.mag)}</div>
      <div class="map-popup__row">${esc(p.coord)}</div>
      <div class="map-popup__row">${esc(p.depth)}</div>
      <div class="map-popup__row">${esc(p.time)}</div>
      <div class="map-popup__row map-popup__src">${esc(p.source)}</div>
    </div>`;
}

interface Props {
  earthquakes: EarthquakeResponse[];
  selectedId?: number | null;
  onSelect?: (eq: EarthquakeResponse) => void;
  center?: [number, number]; // [lat, lon]
}

export function SeismicMap({ earthquakes, selectedId, onSelect, center = [40.65, 29.27] }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<MapLibreMap | null>(null);
  const popupRef = useRef<Popup | null>(null);
  const readyRef = useRef(false);
  // Event handlers read live props through this ref so we never rebind them.
  const liveRef = useRef({ earthquakes, selectedId, onSelect });
  liveRef.current = { earthquakes, selectedId, onSelect };

  // Create the map exactly once. The container has a definite (non-percentage)
  // height in CSS, so MapLibre measures a real size on creation.
  useEffect(() => {
    if (!containerRef.current) return;
    const map = new MapLibreMap({
      container: containerRef.current,
      style: STYLE,
      center: [center[1], center[0]], // MapLibre wants [lng, lat]
      zoom: 5,
      attributionControl: { compact: true },
    });
    mapRef.current = map;
    map.addControl(new NavigationControl({ showCompass: false }), 'top-right');

    map.on('load', () => {
      const { earthquakes: eqs, selectedId: sel } = liveRef.current;
      map.addSource(SOURCE_ID, { type: 'geojson', data: toFeatureCollection(eqs, sel) });
      map.addLayer({
        id: LAYER_ID,
        type: 'circle',
        source: SOURCE_ID,
        paint: {
          'circle-radius': ['get', 'r'],
          'circle-color': ['get', 'color'],
          'circle-opacity': ['case', ['get', 'selected'], 0.75, 0.45],
          'circle-stroke-color': ['get', 'color'],
          'circle-stroke-width': ['case', ['get', 'selected'], 3, 1.5],
          'circle-stroke-opacity': 0.9,
        },
      });
      readyRef.current = true;
      fitTo(map, liveRef.current.earthquakes);
      // Guard against the container settling after first paint (e.g. panel toggle).
      setTimeout(() => map.resize(), 60);
    });

    map.on('click', LAYER_ID, (e) => {
      const f = e.features?.[0];
      if (!f) return;
      const id = Number(f.properties?.id);
      const { earthquakes: eqs, onSelect: sel } = liveRef.current;
      const hit = eqs.find((x) => x.id === id);
      if (hit) sel?.(hit);
      const [lng, lat] = (f.geometry as GeoJSON.Point).coordinates;
      popupRef.current?.remove();
      popupRef.current = new Popup({ closeButton: true, offset: 12 })
        .setLngLat([lng, lat])
        .setHTML(popupHtml(f.properties ?? {}))
        .addTo(map);
    });
    map.on('mouseenter', LAYER_ID, () => {
      map.getCanvas().style.cursor = 'pointer';
    });
    map.on('mouseleave', LAYER_ID, () => {
      map.getCanvas().style.cursor = '';
    });

    const ro = new ResizeObserver(() => map.resize());
    ro.observe(containerRef.current);

    return () => {
      ro.disconnect();
      popupRef.current?.remove();
      popupRef.current = null;
      readyRef.current = false;
      map.remove();
      mapRef.current = null;
    };
    // center is only an initial value; intentionally not a dependency.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Push new data / selection into the existing source without recreating the map.
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !readyRef.current) return;
    const src = map.getSource(SOURCE_ID) as GeoJSONSource | undefined;
    src?.setData(toFeatureCollection(earthquakes, selectedId));
  }, [earthquakes, selectedId]);

  // Re-fit the viewport when the set of plotted events changes (not on selection).
  useEffect(() => {
    const map = mapRef.current;
    if (!map || !readyRef.current) return;
    fitTo(map, earthquakes);
  }, [earthquakes]);

  return (
    <div className="map">
      <div ref={containerRef} className="map__canvas" />
    </div>
  );
}

// Frame all points; single point gets a fixed zoom, many get padded bounds.
function fitTo(map: MapLibreMap, earthquakes: EarthquakeResponse[]) {
  if (earthquakes.length === 0) return;
  if (earthquakes.length === 1) {
    const e = earthquakes[0];
    map.easeTo({ center: [e.longitude, e.latitude], zoom: 7, duration: 0 });
    return;
  }
  let minLng = Infinity, minLat = Infinity, maxLng = -Infinity, maxLat = -Infinity;
  for (const e of earthquakes) {
    minLng = Math.min(minLng, e.longitude);
    maxLng = Math.max(maxLng, e.longitude);
    minLat = Math.min(minLat, e.latitude);
    maxLat = Math.max(maxLat, e.latitude);
  }
  map.fitBounds(
    [[minLng, minLat], [maxLng, maxLat]],
    { padding: 48, maxZoom: 9, duration: 0 },
  );
}
