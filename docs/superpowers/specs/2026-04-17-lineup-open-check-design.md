# Design Spec - Lineup Open Periodic Check & Notification

## Purpose
To automatically notify users when the lineup for the next upcoming F1 race weekend becomes available (opens). This ensures higher user engagement and reminds users to set their teams before the deadline.

## Architecture

### 1. Data Tracking
We will track sent notifications in a separate Firestore collection to avoid modifying the `races` collection and to provide a clean audit trail.

- **Collection**: `lineup_notifications`
- **Document ID**: `lineup_open_${raceId}`
- **Fields**:
  - `raceId`: String
  - `sentAt`: Timestamp
  - `notificationType`: String ("LINEUP_OPEN")

### 2. Components

#### 2.1 Repository Updates
- **`RaceRepository`**:
  - `findNextRace(now: Instant): RaceWeekend?`: Returns the first race where `dateEnd` > `now`, ordered by `dateStart`.
- **`LineupNotificationRepository` (New)**:
  - `isLineupOpenNotificationSent(raceId: String): Boolean`: Checks for the existence of the tracking document.
  - `markLineupOpenNotificationAsSent(raceId: String)`: Creates the tracking document.

#### 2.2 Service Updates
- **`NotificationService`**:
  - `sendLineupOpenNotification(raceWeekend: RaceWeekend): Int`: Logic to iterate through users and send FCM messages.
  - **Notification Payload**:
    - **Title**: "Lineup for ${race.raceName} is now OPEN!"
    - **Body**: "The lineup for the ${race.raceName} is now open. Don't forget to set your team before it closes!"
    - **Data**: `{ "type": "lineupOpen", "raceId": race.raceId }`

#### 2.3 Task Implementation
- **`LineupOpenTask` (New)**:
  - Scheduled at a fixed rate (e.g., every 10 minutes).
  - **Logic**:
    1. Retrieve the next upcoming race.
    2. Check if `now` is within the lineup open window (`dateLineupOpen` <= `now` < `dateLineupClose`).
    3. If within window, verify if the notification has already been sent.
    4. If not sent, trigger the notification and mark it as sent in the repository.

### 3. Error Handling
- If the next race cannot be determined, the task will log a warning and exit.
- If FCM sending fails for specific users, errors will be logged, but the overall task will continue.
- The "mark as sent" operation should happen AFTER the service call to ensure we don't skip notifications if the service crashes, but we should be mindful of potential double-sends in extreme edge cases (at-least-once delivery).

### 4. Testing Strategy
- **Unit Tests**:
  - `LineupOpenTaskTest`: Mock repositories and services to verify the scheduling logic (window checks, skipping if already sent).
  - `LineupNotificationRepositoryTest`: Verify Firestore interactions.
- **Integration Tests**:
  - End-to-end check from task trigger to `NotificationService` call using a test Firestore instance.
