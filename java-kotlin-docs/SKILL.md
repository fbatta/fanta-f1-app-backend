---
name: java-kotlin-docs
description: Look up up-to-date documentation for Java and Kotlin libraries, frameworks, and tools
---

# Java/Kotlin Library Documentation Lookup

## When to use

Use when the user asks about:
- How to use a specific Java or Kotlin library/dependency
- API reference for a library (Spring Boot, Ktor, kotlinx.*, Javalin, etc.)
- Version compatibility between libraries
- Migration guides between library versions
- Configuration examples for a library
- Best practices for a specific library

## Core documentation sources

### Kotlin ecosystem
- **Kotlin language**: `https://kotlinlang.org/docs/`
- **Kotlin coroutines**: `https://kotlinlang.org/docs/coroutines/`
- **Kotlinx Flow**: `https://kotlinlang.org/docs/flow/`
- **Kotlinx serialization**: `https://kotlin.github.io/serialization/`
- **Kotlinx datetime**: `https://kotlin.github.io/datetime/`
- **Kotlinx io**: `https://kotlin.github.io/io/`
- **Kotlinx benchmark**: `https://github.com/Kotlin/kotlinx-benchmark`
- **Arrow (functional programming)**: `https://arrow-kt.io/`
- **Kotlin Test (JUnit 5 engine)**: `https://kotlin.github.io/kotlin-test/`

### Web frameworks
- **Spring Boot**: `https://docs.spring.io/spring-boot/`
- **Spring Framework**: `https://docs.spring.io/spring-framework/`
- **Spring Security**: `https://docs.spring.io/spring-security/`
- **Ktor**: `https://ktor.io/docs/home`
- **Javalin**: `https://javalin.io/docs`
- **Micronaut**: `https://docs.micronaut.io/`
- **Quarkus**: `https://quarkus.io/guides/`
- **Vert.x**: `https://vertx.io/docs/`
- **Play Framework**: `https://www.playframework.com/documentation`

### HTTP clients & servers
- **OkHttp**: `https://square.github.io/okhttp/`
- **Ktor Client**: `https://ktor.io/docs/client-overview.html`
- **Apache HttpClient**: `https://hc.apache.org/documentation.html`
- **Netty**: `https://netty.io/wiki/`
- **Jetty**: `https://www.eclipse.org/jetty/documentation/`

### Data access & databases
- **Hibernate ORM**: `https://hibernate.org/orm/documentation/`
- **JPA (Jakarta Persistence)**: `https://jakarta.ee/specifications/persistence/`
- **jOOQ**: `https://www.jooq.org/doc/`
- **JDBI**: `https://jdbi.org/spec.html`
- **Exposed (JetBrains ORM)**: `https://github.com/JetBrains/Exposed/wiki`
- **SQLDelight**: `https://cashapp.github.io/sqldelight/`
- **Ktorm**: `https://ktorm.apache.org/`
- **HikariCP**: `https://github.com/brettwooldridge/HikariCP`
- **Flyway**: `https://documentation.red-gate.com/flyway/`
- **Liquibase**: `https://docs.liquibase.com/`
- **MongoDB Java Driver**: `https://www.mongodb.com/docs/drivers/java/`
- **Cassandra Driver**: `https://docs.datastax.com/en/developer/java-driver/`
- **Redis (Jedis)**: `https://jedis.github.io/jedis/`
- **Redis (Lettuce)**: `https://lettuce.io/core/release/reference/`
- **Elasticsearch Java Client**: `https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/`
- **Apache Kafka Clients**: `https://kafka.apache.org/documentation/#clientcode`
- **R2DBC**: `https://r2dbc.io/spec/`

### Serialization
- **Jackson**: `https://github.com/FasterXML/jackson-docs`
- **Gson**: `https://github.com/google/gson`
- **Kotlinx Serialization**: `https://kotlin.github.io/serialization/`
- **Kaml (YAML)**: `https://github.com/hildensia/kaml`
- **TOML4J**: `https://github.com/BeckerZ/toml4j`
- **Apache Commons CSV**: `https://commons.apache.org/proper/commons-csv/`
- **Apache Commons IO**: `https://commons.apache.org/proper/commons-io/`
- **Apache Commons Lang**: `https://commons.apache.org/proper/commons-lang/`
- **Apache Commons Collections**: `https://commons.apache.org/proper/commons-collections/`

### Reactive & functional
- **Project Reactor**: `https://projectreactor.io/docs/`
- **RxJava**: `https://rxjava.dev/`
- **RxKotlin**: `https://github.com/ReactiveX/RxKotlin`
- **Kotlinx Flow**: `https://kotlinlang.org/docs/flow/`

### Testing
- **JUnit 5**: `https://junit.org/junit5/docs/current/user-guide/`
- **MockK**: `https://mockk.io/`
- **Kotest**: `https://kotest.io/docs/`
- **AssertJ**: `https://assertj.github.io/doc/`
- **H2 Database**: `https://h2database.com/html/main.html`
- **Testcontainers**: `https://java.testcontainers.org/`
- **WireMock**: `https://wiremock.org/`
- **Spring Test**: `https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html`

### Build tools
- **Gradle**: `https://docs.gradle.org/current/userguide/`
- **Maven**: `https://maven.apache.org/guides/index.html`

### Logging
- **SLF4J**: `https://www.slf4j.org/manual.html`
- **Logback**: `https://logback.qos.ch/manual/`
- **Log4j 2**: `https://logging.apache.org/log4j/2.x/manual/`
- **Kotlinx Logging**: `https://github.com/ktorio/kotlinx-logging`

### API documentation
- **SpringDoc OpenAPI**: `https://springdoc.org/`
- **Swagger/OpenAPI**: `https://swagger.io/docs/`
- **GraphQL Java**: `https://www.graphql-java.com/documentation/`
- **gRPC Java**: `https://grpc.io/docs/languages/java/`
- **Protobuf**: `https://protobuf.dev/`

### Utility libraries
- **Guava**: `https://github.com/google/guava`
- **Apache Commons**: `https://commons.apache.org/`
- **Joda-Time**: `https://www.joda.org/joda-time/`
- **ThreeTenBP**: `https://www.threeten.org/threetenbp/`
- **Kotlinx Coroutines**: `https://kotlinlang.org/docs/coroutines-guide.html`

### Code generation & mapping
- **MapStruct**: `https://mapstruct.org/documentation/`
- **ModelMapper**: `https://modelmapper.org/`
- **Immutables**: `https://immutables.github.io/`
- **Screwdriver**: `https://screwdriver.plugin.gg/`

## How to look up documentation

1. **Identify the library** from the user's question. Map common names to the correct URL above.

2. **Fetch the relevant documentation page** using `webfetch`:
   ```
   webfetch "https://kotlinlang.org/docs/coroutines-basics.html"
   webfetch "https://ktor.io/docs/application-routes.html"
   webfetch "https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/"
   ```

3. **For Maven artifacts**, check `https://mvnrepository.com/artifact/<group>/<artifact>` for version info and dependencies.

4. **For GitHub-hosted docs**, fetch the README or relevant markdown files directly:
   ```
   webfetch "https://raw.githubusercontent.com/mockk/mockk/master/README.md"
   webfetch "https://github.com/mockk/mockk/blob/master/README.md"
   ```

5. **When the exact page is unknown**, fetch the docs homepage first, then navigate to the relevant section.

6. **Present findings** concisely:
   - Show the key API/method signature
   - Include a minimal code example in Kotlin (or Java if no Kotlin example exists)
   - Note any version requirements or compatibility notes
   - Link to the full documentation

## Tips

- Kotlin docs pages often have examples in both Kotlin and Java — prefer Kotlin examples
- Spring Boot docs have a "Search" feature — use the full reference URL and search within the fetched content
- For kotlinx libraries, the `kotlin.github.io` subdomains are the canonical docs
- Gradle Kotlin DSL examples are available alongside Groovy DSL in most docs
- When a library has multiple major versions, check the user's version and fetch docs for that version (e.g., Spring Boot 2.x vs 3.x have different package structures)
