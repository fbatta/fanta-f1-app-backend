# Score calculator design

## Introduction

After a race weekend is over, we need to calculate the scores for each team
that's registered in the app. Ideally, this
process will be as automatic as possible, depending of course on the
availability of the data needed for the
calculation.

We also need to consider that the calculation process could be time consuming,
so it should be done asynchronously.
Also, when the scores are available, a notification should go out to the app's
users.

## Requirements

* System should detect when a race weekend is over

* It should check for availability of data to do the calculations

* Once data is available, calculate scores for each team

* Once calculations are complete, send notification to lobby admin for review

* Lobby admin can view and change scores for each team in the lobby

* Lobby admin confirms scoring

* System makes necessary updates

* Notifications are sent to each team in the lobby once admin has confirmed

## Sequence diagram

```mermaid
sequenceDiagram
    actor User
    actor Lobby admin
    participant Backend
    participant F1 API
    participant Firebase
    Backend ->> F1 API: Get race weekends
    F1 API -->> Backend: List of race weekends
    alt On a schedule
        Backend ->> F1 API: Get race weekend results
        F1 API -->> Backend: Race weekend results
    end
    Backend ->> Firebase: Get teams with lineups
    Firebase -->> Backend: Teams with lineups
    Backend ->> Backend: Calculate scores for each team
    Backend ->> Firebase: Save provisional results
    Backend ->> Lobby admin: Send notification for provisional results
    Lobby admin ->> Backend: Submit changes and confirm
    Backend ->> Firebase: Update results and mark them final
    Backend ->> User: Send notification
    note over Backend, User: Using Firebase messaging
```

## Implementation

### Scheduled process

On an hourly basis, the system should look up the list of race weekends and
check for changes in the schedule. Meanwhile it would also check if any race
weekend is over and add a message to a queue to check the availability
of results for that weekend.

```mermaid
sequenceDiagram
    box rgb(250, 215, 234) Backend
        participant Scheduled task
        participant Queue listener
        participant Calculator service
    end
    participant HashMap
    participant F1 API
    participant Firebase
    participant MQ
    Scheduled task ->> F1 API: Get RWs
    F1 API -->> Scheduled task: RWs
    Scheduled task ->> HashMap: Update RWs
    loop For each RW
        Scheduled task ->> Scheduled task: Check if RW is over
        alt If over
            Scheduled task ->> MQ: Message to calculate results
        end
    end
    MQ -->> Queue listener: Get message to calculate results
    Queue listener ->> Calculator service: Invoke calculator
    Calculator service ->> F1 API: Check availability of results
    alt results available
        Calculator service ->> Firebase: Get teams and lineups
    else results not available
    end
```