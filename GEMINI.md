# GEMINI.md - Fanta F1 App Backend

## Project Overview

The **Fanta F1 App Backend** is the core engine for **IDGAF-1**, a Fantasy Formula 1 mobile application. Built with a modern, reactive Spring Boot architecture, it manages user teams, driver statistics, and complex race weekend scoring calculations.

The service integrates with the **OpenF1** and **Jolpica** APIs to fetch real-time race data and uses **Google GenAI (Gemini)** for generating driver summaries. **Firebase** serves as the primary data store (Firestore) and authentication provider.

## Technical Stack

- **Language:** Kotlin 2.3.20
- **Framework:** Spring Boot 4.0.3 (WebFlux / Reactive)
- **Runtime:** Java 25 (enforced via Toolchain)
- **Asynchrony:** Kotlin Coroutines + Project Reactor (`Flow`, `suspend`)
- **Database:** Firebase Firestore (No local DB)
- **Security:** OAuth2 Resource Server (JWT) + Firebase Auth
- **AI:** Google GenAI SDK (Gemini + Imagen)
- **HTTP Client:** OkHttp 5.3.2
- **Caching:** Caffeine
- **Testing:** JUnit 5, MockK, SpringMockK

## Building and Running

### Prerequisites
- **Java 25**
- **Firebase Service Account:** Must be present at `/credentials/serviceAccount.json` (or configured path).
- **Environment Variables:**
  - `GOOGLE_GENAI_API_KEY`: Required for AI features.
  - `FANTAF1_AUTH_ISSUER_URI`: Required for JWT validation.

### Commands
- **Run Development Server:** `./gradlew bootRun`
- **Build Fat JAR:** `./gradlew bootJar -x test`
- **Run All Tests:** `./gradlew test`
- **Run Specific Test:** `./gradlew test --tests "net.battaglini.fantaf1appbackend.service.DriverServiceTest"`
- **Check Dependencies:** `./gradlew dependencies`

## Project Structure

- `src/main/kotlin/net/battaglini/fantaf1appbackend/`
    - `client/`: External API clients (OpenF1, GenAI).
    - `configuration/`: App settings, Firebase, Security, and Cache configs.
    - `controller/`: REST endpoints (Admin and Notifications).
    - `model/`: Domain models, Firestore DTOs, and Request/Response objects.
    - `repository/`: Reactive Firestore repositories (returning `Flow`).
    - `service/`: Business logic, following the Interface + `*Impl` pattern.
    - `task/`: `@Scheduled` background tasks for score calculations and notifications.
    - `deserializer/` & `serializer/`: Custom Jackson mappings for F1 data types.

## Development Conventions

- **Reactive First:** Use Spring WebFlux and Kotlin Coroutines. Always prefer `suspend` functions and `Flow<T>` over blocking calls.
- **Service Pattern:** Implement business logic in services using the `Interface` + `*Impl` pattern. Inject the interface, not the implementation.
- **Testing:**
    - Use **MockK** for mocking (`coEvery` for suspend functions).
    - Use **MockWebServer** for testing external API integrations.
    - Controllers should be tested using `@WebFluxTest`.
- **Error Handling:** Use custom exceptions located in the `exception` package (e.g., `DriverNotFoundException`).
- **Configuration:** Profiles are split into `application.yaml` (base), `application-local.yaml`, and `application-production.yaml`.

## Background Tasks (Schedulers)

- `RaceWeekendResultsCalculatorTask`: Periodically computes driver points from session results.
- `TeamsResultsCalculatorTask`: Updates user team standings based on new driver scores.
- `NotificationsTask`: Dispatches FCM notifications for various app events.
- `LineupNotificationTask`: Reminds users to set their lineups before sessions close.

## External Data Integration

- **OpenF1:** Primary source for laps, sessions, and live driver data.
- **Jolpica (Ergast):** Secondary source for historical data and race calendars.
- **Google GenAI:** Generates natural language summaries for driver performance.
