# AGENTS.md - Fanta F1 App Backend

## Stack

- **Kotlin 2.3.20** + **Spring Boot 4.0.3** (WebFlux, not MVC)
- **Java 25** enforced via toolchain in `build.gradle.kts`
- **Firebase Admin SDK** (Firestore + Auth) — no local database
- **OkHttp** + **OpenF1 API** + **Jolpica API** for F1 data
- **Google GenAI** (Gemini + Imagen) for AI features
- OAuth2 JWT resource server (Keycloak issuer: `${FANTAF1_AUTH_ISSUER_URI}`)
- Caffeine caching, Kotlin Coroutines + Reactor

## Commands

```bash
./gradlew bootRun          # Run dev server
./gradlew build            # Compile + test
./gradlew test             # Run all tests (JUnit Platform)
./gradlew test --tests "net.battaglini.fantaf1appbackend.service.DriverServiceTest"
./gradlew bootJar -x test  # Build fat JAR without tests (also used in Dockerfile)
./gradlew dependencies     # Resolve deps (cached in Docker build)
```

**CI/CD:** GitHub Actions on PRs to `main` and push to `main`.
- `ci.yml`: runs `./gradlew test --no-daemon`, uploads test results on failure
- `build-and-publish.yml`: builds and pushes Docker image to `ghcr.io` (called as reusable workflow)

No linting, no pre-commit hooks. Tests are the only verification gate.

## Project Structure

```
src/main/kotlin/net/battaglini/fantaf1appbackend/
├── FantaF1AppBackendApplication.kt   # Entry point
├── client/                           # OpenF1Client (@Component), GenAIClient (interface + GenAIClientImpl)
├── configuration/                    # Firebase, Security, Cache, Properties (14 config classes)
├── controller/                       # AdminOperationsController, NotificationsController
├── deserializer/                     # KotlinInstantDeserializer, OpenF1 deserializers
├── enums/                            # RaceWeekendSessionType, TaskType, UserNotificationType, openf1/
├── exception/                        # DriverNotFoundException, InvalidRequestException, NotFoundException
├── model/                            # Domain models (root) + openf1/ DTOs + request/ + response/
├── repository/                       # Firestore repos (11 repos, all return Flow)
├── serializer/                       # KotlinInstantSerializer
├── service/                          # Business logic (most use interface + *Impl pattern)
└── task/                             # @Scheduled tasks (4 tasks: results calc, notifications, lineup)
```

Test mirror: `src/test/kotlin/...` with `*Test` suffix.

## Key Facts

- **Reactive only** — WebFlux, `Flow<T>`, `suspend` functions. Never use blocking calls in suspending functions.
- **Service layer** — Most services use interface + `*Impl` pattern (e.g. `DriverService` + `DriverServiceImpl`). Inject the interface. Exceptions: `QualifyingResultsService`, `RaceWeekendService`, `RaceResultsService`, `PracticeResultsService` are interfaces without separate impl files.
- **Firestore repos return `Flow`** — repository methods are reactive streams.
- **OpenF1Client** is a `@Component` (not interface+impl), uses `@Cacheable` annotations for Caffeine caching.
- **No ktlint/detekt** — rely on Kotlin compiler + `./gradlew build` for style checks.
- **Firebase credentials** mounted at `/credentials/serviceAccount.json` via docker-compose volume.
- **`GOOGLE_GENAI_API_KEY`** env var required for AI features.
- **`FANTAF1_AUTH_ISSUER_URI`** env var required — not a hardcoded URL.
- **Seeding flags** in `application.yaml`: `seeding.drivers` and `seeding.race-weekends` (both default `false`).
- **Results calculator** scheduler: `results-calculator.enable` (default `true`), `results-calculator.dry-run` (default `false`).
- **Firestore pagination** limit is 100 (`firebase.firestore.pagination.query-limit`).

## Testing

- **MockK** (`mockk`, `springmockk`) — use `coEvery`/`coVerify` for suspend functions, `every`/`verify` for regular.
- Tests use `@ExtendWith(MockKExtension::class)` with `@MockK` and `@InjectMockKs` annotations.
- **Spring WebFlux tests** — `@WebFluxTest` for controllers, `@SpringBootTest` for integration.
- Tests use JUnit Platform (`useJUnitPlatform()` in build.gradle.kts).
- No transactional DB cleanup needed (Firestore is mocked).
- Mock external services: `openF1Client`, `genAIService`, repositories.
- Test dependencies include `mockwebserver`, `kotlinx-coroutines-test`, and Spring test starters for security/webflux.

## Security

- Never commit secrets (Firebase keys, API keys, `.env`).
- `.gitignore` covers `*.json`, `*.pem`, `.env`.
- Role-based access via Spring Security OAuth2 resource server.
- Firestore rules in `firestore.rules` — read/write permissions per collection.
- Firestore indexes defined in `firestore.indexes.json`.

## Environment

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

## Configuration Profiles

Three profiles: `application.yaml` (base), `application-local.yaml` (local dev), `application-production.yaml` (prod).
- **Docker/CIs** — runtime forces `SPRING_PROFILES_ACTIVE=production` (via `-Dspring.profiles.active=production` in Dockerfile ENTRYPOINT).
- **`docker-compose.yaml`** — sets `SPRING_PROFILES_ACTIVE=production` via env var.
- **Local dev** (`./gradlew bootRun`) — no profile set, uses base `application.yaml` only.

## Docker

Multi-stage build: `eclipse-temurin:25-jdk` builder → `eclipse-temurin:25-jre` runtime (non-root user `spring`).
Credentials mounted via `docker-compose.yaml`: `./src/main/resources/credentials:/credentials:ro`.
Dockerfile does NOT mount credentials — that's done in docker-compose.
