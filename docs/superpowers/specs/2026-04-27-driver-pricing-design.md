# Design Spec: Dynamic Driver Pricing System

## 1. Overview
Implement an automated driver pricing system that updates driver costs after every race weekend. The system ensures a balanced fantasy economy where the total budget of 200 credits for 4 drivers remains a meaningful constraint.

## 2. Goals
*   **Automation:** Automatically recalculate prices when race results are finalized.
*   **Performance-Driven:** Prices reflect recent driver and team form.
*   **Stability:** Avoid erratic price swings and ensure prices don't drop unfairly when a driver performs well but others perform better.
*   **Economic Balance:** Maintain an average driver cost of ~50 credits to keep the 200-credit budget challenging.

## 3. Pricing Algorithm: Anchored Normalization
The core logic uses a weighted rolling average of recent performances, mapped to a credit range, with protective "smoothing" logic.

### 3.1 Composite Power Score ($P$)
For each driver $D$:
$$P_D = (0.80 \times \text{DriverRollingAvg}) + (0.20 \times \text{TeamRollingAvg})$$

*   **DriverRollingAvg:** Average fantasy points scored by the driver over the **last 3 races**.
*   **TeamRollingAvg:** Average fantasy points scored by the driver's teammate over the **last 3 races**.
*   *Note:* If fewer than 3 races exist (start of season), use all available races. If 0 races, use `initialCost` mapping.

### 3.2 Mapping to Credits
The Power Score (which ranges from 0 to 20) is mapped to a credit range of **20 to 85**.

$$Cost_{raw} = 20 + (P_D / 20) \times (85 - 20)$$

### 3.3 Smoothing & Protections
*   **No Drop on Improvement:** If a driver's composite score $P$ is higher than their score from the previous race, their new cost cannot be lower than their current cost.
*   **Global Deflator:** After calculating all costs, if the grid average exceeds 52 credits, all prices are scaled down by a factor of $(50 / \text{GridAvg})$ to preserve the 200-budget meta.

## 4. Technical Architecture

### 4.1 New Components
*   **`DriverPricingService` (Interface) & `DriverPricingServiceImpl`:**
    *   `calculateAndUpdatesPrices(raceId: String)`: Main entry point.
*   **`PricingProperties`:** Configurable values for weights (0.8/0.2), rolling window size (3), and price floor/ceiling (20/85).

### 4.2 Integration Flow
1.  **Trigger:** `RaceWeekendResultsCalculatorTask` finishes saving results.
2.  **Event:** It sends `RACE_WEEKEND_RESULTS_CALCULATION_COMPLETED` to `taskChannel`.
3.  **Listener:** A dedicated `PricingTask` picks up the message from the `taskChannel`.
4.  **Process:** 
    *   Fetch last 3 `RaceWeekendResult` objects.
    *   Calculate $P_D$ for all active drivers.
    *   Apply mapping, smoothing, and global deflator.
    *   Persist via `DriverCostRepository.createOrUpdateDriversCosts()`.

## 5. Data Flow
1.  **Input:** `RaceWeekendResult` (from Firestore via `RaceWeekendResultRepository`).
2.  **Logic:** `DriverPricingService` performs calculations.
3.  **Output:** `DriverCost` (to Firestore via `DriverCostRepository`).

## 6. Testing Strategy
*   **Unit Tests:** Test the pricing math with various scenarios (teammate outperforming, all drivers improving, start of season).
*   **Integration Tests:** Verify the full flow from `RaceWeekendResult` being saved to `DriverCost` being updated.
*   **Dry Run Mode:** Support a `dry-run` flag in `PricingProperties` to log intended changes without persisting.
