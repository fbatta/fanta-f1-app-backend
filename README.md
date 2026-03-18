# Fanta F1 App Backend

The Fanta F1 App Backend is a robust Spring Boot service designed to power a full-featured Fantasy Formula 1 mobile
application. It acts as the core engine for **IDGAF-1**, managing user teams, driver statistics, race weekend data, and
complex scoring calculations.

By integrating with external data sources like the OpenF1 and Jolpica APIs, this backend automatically fetches real-time
and historical race data (such as lap times, overtakes, qualifying results, and session details). It then processes this
data through scheduled background tasks to calculate driver scores, update user team standings, and manage driver costs
dynamically across the season. The service leverages Firebase for secure user authentication, real-time database
capabilities (Firestore), and structured storage, ensuring seamless synchronization with the IDGAF-1 mobile client.

## Technologies Used

* **Kotlin 2.3.0**
* **Spring Boot 4.0.2** (WebFlux, Security, Cache)
* **Firebase Admin SDK** (Firestore Database, Authentication)
* **OkHttp 5.3.2** (HTTP Client)
* **Coroutines & Reactor** (Asynchronous and Non-Blocking Operations)
* **Caffeine** (Caching)
* **Jackson** (JSON Serialization/Deserialization)

## Features

* **External API Integration:**
    * **[OpenF1 API](https://openf1.org/):** For retrieving live and historical F1 data (sessions, drivers, laps,
      overtakes, stints).
    * **[Jolpica API](https://github.com/jolpica/jolpica-f1):** Additional F1 data provider.
* **Background Tasks (Schedulers):**
    * `RaceWeekendResultsCalculatorTask`: Calculates driver points and results for the race weekend (Practice,
      Qualifying, Race).
    * `TeamsResultsCalculatorTask`: Updates user's team points based on driver performances.
    * `NotificationsTask`: Sends notifications to users.
* **Admin Operations:**
    * Update driver costs manually via API.

## Race Weekend Calculations

The `RaceWeekendResultsCalculatorTask` runs periodically to compute the final scores for all drivers during a race
weekend. Here is how the scoring works:

1. **Session Fetching:** The system first searches for an ongoing or recently completed F1 meeting (within the last 6
   days). If results haven't been calculated yet, it begins processing.
2. **Result Gathering:** It fetches the performance of every driver across all available sessions for that weekend:
    * Combined Practice Results (fastest lap)
    * Qualifying Results
    * Sprint Qualifying Results (if applicable)
    * Race Results
    * Sprint Race Results (if applicable)
3. **Points Assignment:** For each session type, drivers are sorted by their performance (e.g., final position or
   fastest lap time) and assigned points based on their rank:
    * 1st: 20 pts, 2nd: 17 pts, 3rd: 15 pts, 4th: 13 pts, 5th: 11 pts, 6th: 10 pts, 7th: 9 pts, 8th: 8 pts, 9th: 7 pts,
      10th: 6 pts, 11th: 5 pts, 12th: 4 pts, 13th: 3 pts, 14th: 2 pts, 15th: 1 pt. Positions 16 and below receive 0 pts.
4. **Final Driver Score Calculation:** The final score for a driver for the race weekend is the **average (mean) of the
   points earned across all the sessions they participated in** during that weekend.
5. **Storage and Notifications:** Once calculated, the final race weekend results are stored in the database, triggering
   subsequent tasks like team point updates and user notifications.

## Configuration

The application is configured through the `src/main/resources/application.yaml` file. Here are the main configuration
sections:

* `jolpica`: Configuration for the Jolpica API base URL.
* `open-f1`: Configuration for the Open F1 API base URL and version.
* `firebase`: Configuration for the Firebase connection (project ID, credentials path, storage bucket).
* `management`: Configuration for InfluxDB metrics (currently disabled by default).
* `seeding`: Configuration for initial database seeding of drivers and race weekends.
* `results-calculator`: Enable/disable background tasks and dry-run mode.

## API Endpoints

### Admin

* **`POST /admin/drivers/costs`**
    * Updates the cost of drivers for the fantasy league.
    * **Request Body:** JSON representing `UpdateDriversCostsRequest`.

### OpenF1 Test

* **`GET /openf1/races`**
    * Retrieves a flow of race weekends for a given year from the OpenF1 API.
    * **Query Parameters:** `year` (integer)

## Getting Started

### Prerequisites

* Java 25 (configured via toolchain in `build.gradle.kts`)
* Firebase Service Account key (`serviceAccount.json`) placed in the configured credentials path.

### Build and Run

You can run the application using the Gradle wrapper:

```bash
./gradlew bootRun
```

### Running Tests

To run the unit and integration tests:

```bash
./gradlew test
```
