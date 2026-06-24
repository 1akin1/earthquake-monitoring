# eq-detection-service

This service **is** the monolith (`../earthquake-monitoring`) acting as the platform's
producer/core — it is not duplicated here. `docker-compose.yml` builds it directly from the
sibling monolith repo (`build.context: ../earthquake-monitoring`).

## Migration status — done

The monolith is now platform-ready out of the box; no manual pom edits are needed anymore:

1. **Cloud-routable.** The monolith pom already declares the Spring Cloud BOM plus
   `spring-cloud-starter-netflix-eureka-client` and `spring-cloud-starter-config`. They are
   **dormant by default** (`eureka.client.enabled=false`) so the standalone monolith still
   runs with no registry/config server.
2. **No double-handling.** The three in-monolith consumers (`AlertEventListener`,
   `ReportEventListener`, `UserRegionEventListener`) and their support beans
   (`ReportStatistics`, `StatsController`, `LoggingAlertChannel`, `AlertDispatcherConfig`)
   are annotated `@Profile("!platform")`. They run in the standalone monolith but switch
   **off** here, because the dedicated alert/report/user services own those reactions.
3. **Profile + wiring.** `docker-compose.yml` runs this container with
   `SPRING_PROFILES_ACTIVE=platform`, which (a) silences those consumers and (b) turns on
   Eureka registration and central config (`EUREKA_URL`, `CONFIG_SERVER_URL`, and
   `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:19092`).

So the same codebase is a complete standalone app *and* the platform's detection-service —
selected purely by the active Spring profile.
