# Fanta F1 App Backend — Agent Instructions

## Project Overview

The **Fanta F1 App Backend** is the core engine for **IDGAF-1**, a Fantasy Formula 1 mobile application. Built with a modern, reactive Spring Boot architecture, it manages user teams, driver statistics, and complex race weekend scoring calculations.

The service integrates with the **OpenF1** and **Jolpica** APIs to fetch real-time race data and uses **Google GenAI (Gemini)** for generating driver summaries. **Firebase** serves as the primary data store (Firestore) and authentication provider.

## Technical Stack

- **Language:** Kotlin 2.3.20 + **Spring Boot 4.0.3** (WebFlux, not MVC)
- **Runtime:** Java 25 enforced via toolchain in `build.gradle.kts`
- **Asynchrony:** Kotlin Coroutines + Project Reactor (`Flow`, `suspend`)
- **Database:** Firebase Firestore (No local DB) — direct `Firestore` instance, no Spring Data
- **Security:** OAuth2 Resource Server (JWT, Keycloak issuer) + Firebase Auth
- **AI:** Google GenAI SDK (Gemini + Imagen)
- **HTTP Client:** OkHttp 5.3.2
- **Caching:** Caffeine
- **Testing:** JUnit 5, MockK, SpringMockK

## Run commands

```bash
./gradlew bootRun              # Run dev server (needs local profile + Firebase credentials)
./gradlew test                 # Run all tests
./gradlew test --tests "..."   # Run a single test class or method
./gradlew bootJar -x test      # Build fat JAR (used in Dockerfile)
./gradlew dependencyUpdates    # Check for dependency updates (rejects RC/non-stable)
```

## CI/CD

GitHub Actions on PRs to `main` and push to `main`:
- `ci.yml`: runs `./gradlew test --no-daemon`, uploads test results on failure
- `build-and-publish.yml`: builds and pushes Docker image to `ghcr.io` (called as reusable workflow)

No linting, no pre-commit hooks. Tests are the only verification gate.

## Architecture at a glance

Single-module Kotlin/Spring Boot WebFlux app. Package: `net.battaglini.fantaf1appbackend`.

**Layered flow:** Controller → Service → Repository → Firestore (direct `Firestore` instance, no Spring Data).

| Layer | Key directories |
|-------|----------------|
| Controllers | `controller/` — Drivers, RaceWeekends, Teams, Notifications |
| Services | `service/` — interfaces + `*Impl` classes; `RaceWeekendResultsCalculator` is the core scoring engine |
| Tasks | `task/` — 5 scheduled jobs (results calc, pricing, notifications, lineup notifications) |
| Repositories | `repository/` — 12 Firestore repos, all `suspend`/Flow-based |
| Config | `configuration/` — 16 config classes (Firebase, security, schedulers, external APIs, properties) |
| Models | `model/` — Firestore entities + DTOs (`request/`, `response/`, `openf1/`) |
| Clients | `client/` — OpenF1 API, Google Generative AI |

**Firestore collections:** `drivers`, `teams`, `lineups`, `lobbies`, `users`, `race`, `raceWeekendResult`, `raceWeekendRecap`, `driverCost`, `driverSummary`, `driverResult`, lineup notifications.

**External data sources:** OpenF1 API (primary), Jolpica API (fallback). Both configured in `application.yaml`.

## Project Structure

```
src/main/kotlin/net/battaglini/fantaf1appbackend/
├── FantaF1AppBackendApplication.kt   # Entry point
├── client/                           # OpenF1Client (@Component), GenAIClient (interface + GenAIClientImpl)
├── configuration/                    # Firebase, Security, Cache, Properties (16 config classes)
├── controller/                       # Drivers, RaceWeekends, Teams, Notifications
├── deserializer/                     # KotlinInstantDeserializer, OpenF1 deserializers
├── enums/                            # RaceWeekendSessionType, TaskType, UserNotificationType, openf1/
├── exception/                        # DriverNotFoundException, InvalidRequestException, NotFoundException
├── model/                            # Domain models (root) + openf1/ DTOs + request/ + response/
├── repository/                       # 12 Firestore repos (all return Flow)
├── serializer/                       # KotlinInstantSerializer
├── service/                          # Business logic (most use interface + *Impl pattern)
└── task/                             # @Scheduled tasks (5 tasks: results calc, pricing, notifications, lineup)
```

Test mirror: `src/test/kotlin/...` with `*Test` suffix.

## Development Conventions

- **Reactive First:** Use Spring WebFlux and Kotlin Coroutines. Always prefer `suspend` functions and `Flow<T>` over blocking calls.
- **Service Pattern:** Implement business logic in services using the `Interface` + `*Impl` pattern. Inject the interface, not the implementation.
  - Exceptions: `QualifyingResultsService`, `RaceWeekendService`, `RaceResultsService`, `PracticeResultsService`, `RaceWeekendResultsCalculator` are interfaces/classes without separate impl files.
- **Testing:**
  - Use **MockK** for mocking (`coEvery`/`coVerify` for suspend functions, `every`/`verify` for regular).
  - Use **MockWebServer** for testing external API integrations.
  - Controllers should be tested using `@WebFluxTest`.
- **Error Handling:** Use custom exceptions located in the `exception` package (e.g., `DriverNotFoundException`).
- **Configuration:** Profiles are split into `application.yaml` (base), `application-local.yaml` (local dev), and `application-production.yaml` (prod).

## Testing

- **Framework:** JUnit 5 + MockK (`@MockK`, `@InjectMockKs`) + SpringMockK (`@MockkBean`)
- **Coroutines:** `kotlinx.coroutines.test.runTest`, mock `delay` via `mockkStatic("kotlinx.coroutines.DelayKt")`
- **Unit tests:** Pure MockK — no Spring context, no Firestore. Run with `./gradlew test` with no infrastructure.
- **Controller tests:** `@WebFluxTest` + `@MockkBean` for services. No Firestore needed.
- **Smoke test:** `FantaF1AppBackendApplicationTests` uses `@SpringBootTest` — requires Firebase credentials to load context.
- **Test factories:** Shared helpers in `service/TestFactories.kt` (Driver, Team, RaceWeekend, User).
- No transactional DB cleanup needed (Firestore is mocked).
- Mock external services: `openF1Client`, `genAIService`, repositories.

## Configuration & environment

Three profiles: `application.yaml` (base), `application-local.yaml` (local dev), `application-production.yaml` (prod).
- **Docker/CIs** — runtime forces `SPRING_PROFILES_ACTIVE=production` (via `-Dspring.profiles.active=production` in Dockerfile ENTRYPOINT).
- **`docker-compose.yaml`** — sets `SPRING_PROFILES_ACTIVE=production` via env var.
- **Local dev** (`./gradlew bootRun`) — no profile set, uses base `application.yaml` only.

| Config Key | Required | Default |
|------------|----------|---------|
| FANTAF1_AUTH_ISSUER_URI | Yes | - |
| GOOGLE_GENAI_API_KEY | Yes | - |
| firebase.project-id | No | `fantaf1-beitz25` |
| firebase.credentials-path | No | `/credentials/serviceAccount.json` |
| firebase.storage-bucket | No | `fantaf1-beitz25.firebasestorage.app` |
| firebase.database-id | No | `(default)` |
| open-f1.base-url | No | `https://api.openf1.org` |
| open-f1.api-version | No | `v1` |
| jolpica.base-url | No | `https://api.jolpi.ca/ergast/f1/` |
| google.genai.chat-model | No | `gemini-flash-latest` |
| google.genai.image-model | No | `imagen-3.0-generate-002` |
| seeding.drivers | No | `false` |
| seeding.race-weekends | No | `false` |
| results-calculator.enable | No | `true` |
| results-calculator.dry-run | No | `false` |
| notifications.lineup.close-reminder-time-before | No | `12h` |

## Security

- Never commit secrets (Firebase keys, API keys, `.env`).
- `.gitignore` covers `*.json`, `*.pem`, `.env`.
- Role-based access via Spring Security OAuth2 resource server. Role extracted from `user_role` JWT claim → `ROLE_<UPPERCASE>`. Admin endpoints protected by `@PreAuthorize`.
- Firestore rules in `firestore.rules` — read/write permissions per collection.
- Firestore indexes defined in `firestore.indexes.json`.
- API docs: SpringDoc OpenAPI at `/docs` (Swagger UI). API spec at `/v1/api-docs`.

## Docker

Multi-stage build: `eclipse-temurin:25-jdk` builder → `eclipse-temurin:25-jre` runtime (non-root user `spring`).
Credentials mounted via `docker-compose.yaml`: `./src/main/resources/credentials:/credentials:ro`.
Dockerfile does NOT mount credentials — that's done in docker-compose.

## Gotchas

- **No Spring Data Firestore.** Repositories inject `Firestore` directly and use manual queries + Jackson `ObjectMapper` for deserialization.
- **All repository methods are `suspend` / Flow-based.** Services calling them must be coroutine-aware.
- **Custom Jackson serializers** for `Instant` and OpenF1 timestamp formats — don't assume default Kotlin serialization.
- **Smoke test (`@SpringBootTest`)** will fail without Firebase credentials. Use unit/controller tests for fast feedback.
- **Scheduled tasks** are controlled by `results-calculator.enable/dry-run` and `pricing.enable/dry-run` properties.
- **Dependency updates plugin** rejects release candidates and non-stable versions automatically.
- **Firestore pagination** limit is 100 (`firebase.firestore.pagination.query-limit`).
- **OpenF1Client** is a `@Component` (not interface+impl), uses `@Cacheable` annotations for Caffeine caching.
