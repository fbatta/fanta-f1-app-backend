# AGENTS.md - Fanta F1 App Backend Guidelines

This document provides instructions for agentic coding agents (e.g., Cursor, Copilot-assisted agents) operating in this repository.

## Build Commands

### Project Structure
- `src/main/kotlin/` - Main application source code
- `src/test/kotlin/` - Test code
- `src/main/resources/` - Configuration and properties

### Build Commands
```bash
# Build
./gradlew build

# Run all tests
./gradlew test

# Run a single test
./gradlew test --tests "net.battaglini.fantaf1appbackend.controller.AdminOperationsControllerTest"

# Clean and rebuild
./gradlew clean build
```

## Code Style Guidelines

### Imports
- Sort imports by category: standard library → framework dependencies → local packages
- Group imports within a package together
- No blank lines between import groups
- Use single-line imports where possible; split long imports at 88 characters
- Import only what you use; avoid wildcard imports
- Example order:
  ```kotlin
  import kotlinx.coroutines.flow.*
  import org.springframework.stereotype.Service
  import net.battaglini.fantaf1appbackend.model.Driver
  import net.battaglini.fantaf1appbackend.exception.DriverNotFoundException
  import org.slf4j.LoggerFactory
  ```

### Formatting
- Indentation: 4 spaces
- Maximum line length: 100 characters
- Single spacing between lines
- Blank lines before/after methods and top-level functions
- Trailing commas in multi-line data class declarations and collection literals
- Example:
  ```kotlin
  data class Driver(
      val driverId: String,
      val driverNumber: Int,
  )
  ```

### Types & Data Classes
- Prefer `data class` for record-like types: `Driver`, `DriverResult`, request/response DTOs
- Use sealed classes for exhaustive type hierarchies
- Use `@OptIn(ExperimentalUuidApi::class)` when using Kotlin UUID preview API
- Avoid unnecessary getters; data classes provide auto-generated ones
- Use `Flow<T>` for reactive streams; prefer suspension over callbacks

### Naming Conventions
- Classes: PascalCase (`Driver`, `UserService`)
- Files: Match class name (`Driver.kt`)
- Functions: snake_case for internal logic (`calculateDriverId`, `seedDrivers`)
- Package: reverse-DNS style (`net.battaglini.fantaf1appbackend.*`)
- Constants: uppercase_snake_case if needed (`MAX_SCORE`)
- Variables/delocators: camelCase for Java compatibility (`driverId`, `teamName`

### Error Handling
- Use checked exceptions sparingly; prefer throwing specific unchecked exceptions
- Define custom exception classes per domain: `DriverNotFoundException`, `InvalidRequestException`
- Catch specific exceptions, rethrow with context or handle gracefully
- Log errors with StackTrace; use debug level for expected issues:
  ```kotlin
  try {
      driverRepository.findById(id)
  } catch (e: DriverNotFoundException) {
      throw InvalidRequestException(e.message)
  } catch (e: Exception) {
      LOGGER.error("Unexpected error", e)
      throw RuntimeException(e.message)
  }
  ```
- Never swallow exceptions without logging
- Use `@Suppress("SWITCH_EXHAUSTIVE_CHECK_WARNING")` when necessary for Kotlin's exhaustivity check, documenting why the case is impossible

### Coroutines
- Always suspend top-level functions and suspend functions in classes
- Use `suspend` for async operations; avoid blocking calls in suspending functions
- Use `flow {}` collect in `awaitSingle()` or materialized collection for side effects
- Prefer `CoroutineScope` over top-level dispatchers; inject scope via constructor
- Cancel scopes properly in `@PreDestroy` or finally-blocks

### Logging
- Use SLF4J via `LoggingService` or `LoggerFactory`
- Appropriate levels: `info` (expected), `debug` (detailed), `error` (issues)
- Avoid logging sensitive data
- Include contextual information in log messages

## Testing Guidelines

### Test Structure
- Each test is a method decorated with `@Test`
- Use descriptive test names starting with the expected behavior:
  ```kotlin
  fun `updateDriversCosts should return 200 OK when request is valid`() { }
  ```
- Place test files in same package as source code under `*Test` suffix
- Use `@WebFluxTest` for controller tests; `@SpringBootTest` for integration tests

### Test Best Practices
- Use MockK for mocking: `mockkBean()`, `coEvery()`, `coVerify()`
- Arrange-Act-Assert pattern: setup → invoke → verify
- Keep tests independent; no shared state between tests
- Use transactional for database cleanup when testing repository layer
- Mock external services: `openF1Client`, `firebaseClient`
- Test error paths explicitly with expected exceptions

### Example Test
```kotlin
@Test
fun `getDrivers returns active drivers when filter is applied`() {
    val repository = mockk<DriverRepository>()
    every { repository.getDrivers() } returns activeDrivers
    val flow = DriverService(driverRepository = repository).getDriversInSessions(emptyList())
    val result = flow.toList()
    assertTrue(result.size == activeDrivers.size)
    verify(repository).getDrivers()
}
```

## Security

- Never commit sensitive secrets (tokens, keys, credentials)
- Use `.gitignore` for `*.json`, `*.pem`, `.env`
- Always validate user input to prevent injection attacks
- Use Spring Security best practices: role-based access control
- Validate Firebase SDK initialization before operations

## API Design

- Follow RESTful conventions: CRUD endpoints with standard HTTP methods
- Return appropriate status codes: 200 (success), 201 (created), 400 (bad request), 401 (unauthorized), 404 (not found), 500 (server error)
- Use data classes for request/response DTOs
- Use `@RequestBody` for POST/PUT, `@RequestParam` for queries

## Security

- Never commit sensitive secrets (tokens, keys, credentials)
- Use `.gitignore` for `*.json`, `*.pem`, `.env`
- Always validate user input to prevent injection attacks
- Use Spring Security: role-based access control

## Common Pitfalls

1. **Swallowing exceptions** - Always handle or rethrow with context
2. **Memory leaks** - Properly dispose scopes; cancel flows before scope cancellation
3. **Race conditions** - Use `MutableStateFlow` or `Mutex` for mutable state
4. **Unhandled errors** - Log errors; don't let stack traces hit production unhandled
5. **Missing validation** - Validate inputs; use `@Valid` or manual checks

## Environment

| Config Key | Required | Default |
|------------|----------|---------|
| firebase.project.id | Yes | - |
| service-account-file.path | Yes | - |
| openf1.api.url | Yes | - |
| seeding.drivers | No | false |
| results-calculator.enabled | No | true |

## Debugging

```bash
# Enable debug logging
./gradlew run --debug -Dlogging.level.net.battaglini=DEBUG

# Profile application
./gradlew bootRun -Dspring.main.banner-mode=off
```

## Resources

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Kotlin Docs](https://kotlinlang.org/docs/home.html)
- [OpenF1 API](https://openf1.org/)
- [Firebase Admin SDK](https://github.com/firebase/firebase-admin-java)
