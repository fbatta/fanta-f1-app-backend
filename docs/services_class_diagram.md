classDiagram
    class NotificationService {
        -FirebaseMessaging firebaseMessaging
        -LobbyRepository lobbyRepository
        -UserService userService
        +processRaceWeekendCalculationCompletedNotification(RaceWeekendResult) Int
    }

    class GenAIService {
        -GenAIClient genAIClient
        -Clock clock
        +generateDriverSummary(String, Double) Flow~String~
        +generateRaceRecap(String) Flow~String~
    }

    class PracticeResultsService {
        -OpenF1Client openF1Client
        -DriverService driverService
        +getDriversResultsForCombinedPractice(RaceWeekend) Flow~DriverPracticeResult~
    }

    class RaceResultsService {
        -OpenF1Client openF1Client
        -DriverService driverService
        +getResultsForRace(RaceWeekend, Boolean) Flow~DriverRaceResult~
        -getDriverResultForRace(...)
    }

    class RaceWeekendService {
        -OpenF1Client openF1Client
        -RaceRepository raceRepository
        -SeedingProperties seedingProperties
        -Clock clock
        +seedRaceWeekends()
        +onStart()
    }

    class DriverService {
        -OpenF1Client openF1Client
        -GenAIService genAIService
        -DriverRepository driverRepository
        -DriverCostRepository driverCostRepository
        -DriverSummaryRepository driverSummaryRepository
        -RaceWeekendResultRepository raceWeekendResultRepository
        -RaceRepository raceRepository
        -SeedingProperties seedingProperties
        -Clock clock
        -TimeZone timeZone
        +seedDrivers()
        +updateDriversCosts(UpdateDriversCostsRequest)
        +updateDriverSummary(String)
        +getDriversInSessions(List~Int~) Flow~Driver~
        +calculateDriverAverageScore(Int, String?, String?) Result
    }

    class QualifyingResultsService {
        -OpenF1Client openF1Client
        -DriverService driverService
        +getDriversResultsForQualifying(RaceWeekend, Boolean) Flow~DriverQualifyingResult~
    }

    class UserService {
        -UserRepository userRepository
        -TeamRepository teamRepository
        +getUsersByLobbyId(String) Flow~User~
    }

    class OpenF1Client {
        <<Client>>
        +getRaces(Int) Flow~OpenF1MeetingResponse~
        +getSessions(Int) Flow~OpenF1SessionResponse~
        +getResults(...)
        +getQualifyingResults(...)
        +getStartingGrid(...)
        +getLaps(...)
        +getDrivers(...)
    }

    class GenAIClient {
        <<Client>>
        +generateContentNoThinking(String, List~String~) Flow~String~
    }

    NotificationService --> UserService
    NotificationService --> LobbyRepository
    GenAIService --> GenAIClient
    PracticeResultsService --> OpenF1Client
    PracticeResultsService --> DriverService
    RaceResultsService --> OpenF1Client
    RaceResultsService --> DriverService
    RaceWeekendService --> OpenF1Client
    RaceWeekendService --> RaceRepository
    DriverService --> OpenF1Client
    DriverService --> GenAIService
    DriverService --> DriverRepository
    DriverService --> DriverCostRepository
    DriverService --> DriverSummaryRepository
    DriverService --> RaceWeekendResultRepository
    DriverService --> RaceRepository
    QualifyingResultsService --> OpenF1Client
    QualifyingResultsService --> DriverService
    UserService --> UserRepository
    UserService --> TeamRepository

    class DriverRepository { <<Repository>> }
    class RaceRepository { <<Repository>> }
    class LobbyRepository { <<Repository>> }
    class UserRepository { <<Repository>> }
    class TeamRepository { <<Repository>> }
    class DriverCostRepository { <<Repository>> }
    class DriverSummaryRepository { <<Repository>> }
    class RaceWeekendResultRepository { <<Repository>> }
