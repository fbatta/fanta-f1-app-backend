# Design Spec: Manual Driver Pricing Endpoint

## 1. Overview
Extend the dynamic driver pricing system to allow manual triggers through an admin endpoint. This provides flexibility for administrative corrections or forced updates without waiting for the automated task.

## 2. Goals
*   **Manual Trigger:** Provide a REST endpoint for administrators to trigger pricing updates.
*   **Selective Updates:** Support updating all drivers or a specific subset identified by their acronyms.
*   **Smart Anchor Detection:** Automatically identify the latest race with results if no specific race is targeted.
*   **Grid Balance Maintenance:** Ensure that even partial updates respect the "Global Deflator" logic to keep the grid average within the target budget.

## 3. Implementation Details

### 3.1 Request Model
New DTO `UpdateDriversPricesRequest`:
```kotlin
data class UpdateDriversPricesRequest(
    val acronyms: List<String>? = null,
    val updateAllDrivers: Boolean = false
)
```

### 3.2 Service Logic (DriverPricingService)
Update the interface to be more flexible:
```kotlin
suspend fun calculateAndUpdatePrices(
    lastRaceId: String? = null,
    acronyms: List<String>? = null,
    updateAll: Boolean = false
)
```

**Implementation Refinement:**
*   **Race Resolution:** If `lastRaceId` is null, the service will fetch all races for the current year, sort them by date descending, and find the first one that has a saved `RaceWeekendResult` in the repository.
*   **Driver Filtering:** 
    *   If `updateAll` is true, calculate for all active drivers.
    *   If `acronyms` is provided, calculate only for those drivers.
    *   If both are provided, `updateAll` takes precedence.
*   **Deflator Logic:** To keep the economy balanced during a partial update:
    1.  Fetch *current* costs for all drivers.
    2.  Calculate *new* candidate costs for the target subset.
    3.  Create a "Projected Grid" where target drivers have their *new* costs and others have their *current* costs.
    4.  Calculate the average of this Projected Grid.
    5.  If it exceeds the threshold (e.g., 52cr), calculate the deflator factor based on this average.
    6.  Apply the deflator *only to the drivers being updated*.

### 3.3 Controller Endpoint
Add to `AdminOperationsController`:
*   `POST /admin/drivers/prices`
*   Calls `driverPricingService.calculateAndUpdatePrices`.

## 4. Technical Architecture

### 4.1 Modified Components
*   **`DriverPricingService` & `DriverPricingServiceImpl`**: Updated signatures and logic.
*   **`AdminOperationsController`**: New `@PostMapping`.

## 5. Testing Strategy
*   **Unit Tests (`DriverPricingServiceTest`):**
    *   Test latest race detection when `raceId` is null.
    *   Test selective update (only VER updated, PER stays same).
    *   Test deflator behavior during partial updates.
*   **Controller Tests (`AdminOperationsControllerTest`):**
    *   Verify the new endpoint calls the service with correct parameters.
