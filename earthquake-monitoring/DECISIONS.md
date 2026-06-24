# Architecture Decision Record

Short, dated rationale for the choices that shaped this project. Newest decisions extend,
rather than replace, earlier ones unless stated.

## 1. Hexagonal (ports & adapters) architecture
**Context.** The system ingests from external feeds, persists, caches and publishes events,
and must stay testable.
**Decision.** Domain at the centre; the outside world (web, JPA, Redis, Kafka, feeds) reaches
it only through ports, implemented by adapters in `infrastructure`.
**Consequence.** The domain has zero framework imports and is unit-testable without Spring;
swapping an adapter (e.g. logging alert channel → SMS) touches one class.

## 2. Domain kept framework-free
**Decision.** No Spring/JPA/Lombok annotations in `domain`. Persistence uses separate JPA
entities mapped to/from domain models.
**Consequence.** Pure logic compiles and runs standalone (used throughout for fast tests);
the model never leaks ORM concerns.

## 3. STA/LTA detection behind a port
**Decision.** The short-term/long-term average detector is a domain service behind an
`EarthquakeDetector` port; the controller passes raw signal windows.
**Consequence.** The detection algorithm is swappable and testable in isolation.

## 4. Strategy for risk scoring
**Decision.** Risk score per magnitude band is a `RiskScoringStrategy`; the service picks the
strategy that `supports()` the magnitude.
**Consequence.** A new band/formula is a new strategy, no edits to existing ones (OCP).

## 5. Self-selecting factories
**Decision.** Factories (disaster handlers, report renderers, …) hold all candidates and ask
each `supports(...)`; assembly is one config class.
**Consequence.** Extension = new class + one bean line; the factory never changes.

## 6. Builder for the immutable report
**Decision.** `SeismicReport` is immutable and assembled via a builder that folds earthquakes
into running aggregates.
**Consequence.** No half-built reports escape; aggregation logic lives in one place.

## 7. Template Method for rendering + Locale.US
**Decision.** `ReportRenderer.render()` is a `final` skeleton (header→summary→body→footer,
with an empty-report branch); subclasses fill steps. All numbers use `Locale.US`.
**Consequence.** Output order can't be broken by a subclass; a Turkish-locale JVM never turns
`4.80` into `4,80` (which had broken format-sensitive output).

## 8. Chain of Responsibility for signal validation
**Decision.** Raw signals pass a chain (finite samples → window length → dead channel →
clipping) before detection; first failure short-circuits and maps to **HTTP 422**.
**Consequence.** Unusable signals fail loudly and specifically instead of silently producing
"no earthquake".

## 9. Command for alerts (retry + rollback)
**Decision.** Each alert action is an `AlertCommand` (execute/undo); the dispatcher retries
transient failures and rolls a partially-applied batch back via `undo()`.
**Consequence.** Alerts aren't lost on a blip and a batch never lands half-applied.

## 10. Decorator for raw-feed conditioning
**Decision.** Each feed is wrapped in a stack: min-magnitude filter → depth-defaulting
(enrichment) → de-duplication, all behind the `SeismicFeedPort` interface.
**Consequence.** Conditioning is composable and transparent; callers see only the port.

## 11. Facade over the monitoring flow
**Decision.** `DisasterFacade.runMonitoringCycle()` composes import + report behind one call.
**Consequence.** A simple entry point without merging the use cases into a god object.

## 12. Kafka Observer fan-out with distinct groups
**Decision.** Detected events are published to `earthquake.detected`; alert/report/user
consumers each use their own `groupId`, so all see every event.
**Consequence.** Reactions are decoupled and independently scalable — and later splittable
into services (Phase 2) with no code change to the fan-out.

## 13. Publish inside the transaction (known tradeoff)
**Context.** Events are currently published within the DB transaction.
**Decision.** Accept this for simplicity now.
**Consequence.** A rolled-back transaction could still emit an event. Production should use a
transactional outbox or `@TransactionalEventListener(AFTER_COMMIT)`. Documented, not hidden.

## 14. JDK-only JWT (HS256)
**Decision.** Implement signing/verification with `javax.crypto.Mac` + Base64URL rather than a
third-party JWT library. The parser never trusts the token header — it always recomputes the
HMAC with the server secret.
**Consequence.** One fewer dependency, full control, and immunity to `alg:none`/algorithm-
confusion attacks. The core is fully unit-tested.

## 15. Role-based access: PUBLIC / SCIENTIST / ADMIN
**Decision.** Reads are open to any authenticated role; analysis/writes need SCIENTIST+;
deletes need ADMIN. Stateless sessions, CSRF off (no cookies).
**Consequence.** Least-privilege access aligned with the roadmap's roles.

## 16. Observability via Actuator + Micrometer
**Decision.** Expose health/info/metrics + a Prometheus scrape endpoint; health/info public.
**Consequence.** Standard probes and metrics with no bespoke code.

## 17. Phase 2 split + event interop without producer change
**Decision.** Extract the three consumers into services sharing a tiny `eq-events` contract.
Consumers set `spring.json.use.type.headers=false` + a default type, so they deserialize the
monolith's existing JSON unchanged.
**Consequence.** The split required (almost) no producer change; services depend only on the
event contract, not on each other.

## 18. Flyway owns the schema; Hibernate validates
**Decision.** Migrations are authoritative (`ddl-auto: validate`).
**Consequence.** Schema changes are reviewed, versioned and reproducible.

## 19. Redis caching
**Decision.** Cache read-heavy queries (e.g. earthquake lists) in Redis; evict on writes.
**Consequence.** Lower DB load; eviction keeps reads consistent after mutations.

## 20. Detection-service migration via a Spring profile (not deletion)
**Context.** The monolith doubles as the platform's detection-service. The Phase 2 plan said to
delete the three in-monolith consumers once dedicated services existed.
**Decision.** Keep them, but annotate the consumer-side beans (`AlertEventListener`,
`ReportEventListener`, `UserRegionEventListener`, `ReportStatistics`, `StatsController`,
`LoggingAlertChannel`, `AlertDispatcherConfig`) `@Profile("!platform")`. The Spring Cloud
client deps ship in the pom but are dormant unless the `platform` profile is active.
**Consequence.** One codebase is both a complete standalone monolith *and* the platform's
detection-service, selected by `SPRING_PROFILES_ACTIVE`. No duplicated producer, nothing
deleted, no half-app left behind.

## 21. CORS for the browser SPA
**Decision.** Enable CORS in `SecurityConfig` with origins from `app.cors.allowed-origins`
(env-overridable), allowing only the `Authorization`/`Content-Type` headers and leaving
`allowCredentials` off — the JWT rides the Authorization header, not a cookie.
**Consequence.** A separate-origin frontend can call the API; preflight `OPTIONS` is permitted
and the JWT filter already passes through token-less requests, so nothing else changes.
