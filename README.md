# Aviation API Wrapper - Production-Ready Microservice

A production-ready Spring Boot microservice that integrates with aviation data APIs to retrieve airport information by ICAO code. This service implements comprehensive resilience patterns, observability features, and clean architecture principles.

## Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [API Documentation](#api-documentation)
- [Resilience Patterns](#resilience-patterns)
- [Observability](#observability)
- [Architecture Decisions](#architecture-decisions)
- [Error Handling](#error-handling)
- [Future Improvements](#future-improvements)

## Overview
w
This microservice provides a robust wrapper around aviation data APIs (specifically aviationapi.com) to fetch airport information using ICAO codes. The implementation prioritizes production readiness with emphasis on:

- **Scalability**: Stateless design, clean service layering
- **Resilience**: Circuit breaker, retry logic, rate limiting, graceful degradation
- **Extensibility**: Provider-agnostic design allowing easy API provider switching
- **Observability**: Comprehensive logging, metrics, and health endpoints

## Architecture

The application follows a layered architecture pattern:

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                      │
│              (REST API, Input Validation)                │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                     Service Layer                        │
│           (Business Logic, ICAO Validation)              │
└─────────────────────────────────────────────────────────┘
                            │
┌─────────────────────────────────────────────────────────┐
│                     Client Layer                         │
│    (External API Integration, Provider Abstraction)      │
│    Circuit Breaker │ Retry │ Rate Limiter               │
└─────────────────────────────────────────────────────────┘
                            │
                    ┌───────┴────────┐
                    │  Aviation API  │
                    └────────────────┘
```

### Key Components

1. **Controller Layer** (`AirportController`)
   - Exposes REST endpoints
   - Handles HTTP request/response
   - Delegates to service layer

2. **Service Layer** (`AirportService`)
   - Contains business logic
   - Validates ICAO code format
   - Normalizes input (uppercase, trim)
   - Provider-agnostic

3. **Client Layer** (`AviationDataClient` interface)
   - Abstract interface for aviation data providers
   - `AviationApiClientImpl` - aviationapi.com implementation
   - Easy to swap providers without changing service layer
   - Includes resilience patterns

4. **Exception Handling** (`GlobalExceptionHandler`)
   - Centralized error handling
   - Consistent error responses
   - Proper HTTP status codes
   - Trace IDs for debugging

## Key Features

### 🛡️ Resilience Patterns

- **Circuit Breaker**: Prevents cascading failures by opening circuit after threshold failures
- **Retry Logic**: Automatic retries with exponential backoff for transient failures
- **Rate Limiting**: Protects against excessive API calls
- **Graceful Degradation**: Meaningful error messages when service unavailable

### 📊 Observability

- **Structured Logging**: Debug-level logging for application, INFO for frameworks
- **Metrics**: Prometheus-compatible metrics via Spring Boot Actuator
- **Health Checks**: Circuit breaker and rate limiter health indicators
- **Trace IDs**: Unique trace IDs in error responses for request tracking

### 🔌 Extensibility

- **Provider Abstraction**: Easy to switch aviation data providers
- **Decoupled Layers**: Service layer independent of external API structure
- **Configurable**: All timeouts, retry counts, circuit breaker thresholds configurable

### 🔒 Production-Ready

- **Input Validation**: ICAO code format validation
- **Proper Timeouts**: Connection and read timeouts configured
- **Graceful Shutdown**: Server shutdown handling
- **Comprehensive Tests**: Unit tests and integration tests with WireMock

## Technology Stack

- **Java 17**: Modern Java LTS version
- **Spring Boot 3.5.6**: Latest Spring Boot with production features
- **Resilience4j 2.2.0**: Circuit breaker, retry, rate limiter
- **Spring Boot Actuator**: Health checks, metrics, monitoring
- **Micrometer**: Metrics collection and Prometheus integration
- **SpringDoc OpenAPI**: API documentation (Swagger UI)
- **WireMock**: Integration testing with mocked external APIs
- **JUnit 5**: Unit and integration testing
- **Lombok**: Reducing boilerplate code
- **Gradle**: Build automation

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle (or use included Gradle wrapper)
- Internet connection (for accessing aviation API)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd behometask
   ```

2. **Build the project**
   ```bash
   ./gradlew clean build
   ```

   On Windows:
   ```powershell
   .\gradlew.bat clean build
   ```

## Running the Application

### Using Gradle

```bash
./gradlew bootRun
```

On Windows:
```powershell
.\gradlew.bat bootRun
```

### Using the JAR

```bash
./gradlew build
java -jar build/libs/behometask-0.0.1-SNAPSHOT.jar
```

### Configuration

The application runs on port 8080 by default. Key configurations can be modified in `src/main/resources/application.properties`:

- `aviation.api.base-url`: External aviation API base URL
- `aviation.api.connection-timeout`: Connection timeout in milliseconds
- `aviation.api.read-timeout`: Read timeout in milliseconds
- Circuit breaker, retry, and rate limiter configurations

### Verification

Once running, verify the application is healthy:

```bash
curl http://localhost:8080/actuator/health
```

## Running Tests

### Run All Tests

```bash
./gradlew test
```

On Windows:
```powershell
.\gradlew.bat test
```

### Run Tests with Coverage

```bash
./gradlew test jacocoTestReport
```

### Test Structure

- **Unit Tests**: `AirportServiceTest` - Tests business logic and validation
- **Integration Tests**: `AirportControllerIntegrationTest` - End-to-end tests with WireMock
  - Successful airport retrieval
  - Not found scenarios
  - Invalid input validation
  - Retry mechanism verification
  - Service unavailability handling
  - Case-insensitive ICAO code handling

## API Documentation

### Swagger UI

Once the application is running, access the interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

### OpenAPI Spec

JSON specification available at:
```
http://localhost:8080/api-docs
```

### Endpoints

#### Get Airport by ICAO Code

```http
GET /api/v1/airports/{icaoCode}
```

**Path Parameters:**
- `icaoCode` (string, required): 4-letter ICAO airport code (e.g., KJFK, EGLL, LFPG)

**Success Response (200 OK):**
```json
{
  "icaoCode": "KJFK",
  "iataCode": "JFK",
  "name": "John F Kennedy International Airport",
  "city": "New York",
  "country": "United States",
  "location": {
    "latitude": 40.6398,
    "longitude": -73.7789
  },
  "timezone": "America/New_York",
  "elevation": 13
}
```

**Error Responses:**

- **400 Bad Request**: Invalid ICAO code format
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid ICAO code format: '123'. ICAO codes must be exactly 4 uppercase letters.",
  "path": "/api/v1/airports/123",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

- **404 Not Found**: Airport not found
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Airport with ICAO code 'XXXX' not found",
  "path": "/api/v1/airports/XXXX",
  "traceId": "550e8400-e29b-41d4-a716-446655440001"
}
```

- **429 Too Many Requests**: Rate limit exceeded
- **503 Service Unavailable**: External API unavailable or circuit breaker open

### Example Requests

```bash
# Successful request
curl http://localhost:8080/api/v1/airports/KJFK

# Case-insensitive (automatically normalized to uppercase)
curl http://localhost:8080/api/v1/airports/kjfk

# Invalid format (too short)
curl http://localhost:8080/api/v1/airports/ABC

# Not found
curl http://localhost:8080/api/v1/airports/XXXX
```

## Resilience Patterns

### Circuit Breaker Configuration

```properties
resilience4j.circuitbreaker.instances.aviationApi.sliding-window-size=10
resilience4j.circuitbreaker.instances.aviationApi.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.aviationApi.wait-duration-in-open-state=30s
```

**How it works:**
1. Monitors last 10 calls (sliding window)
2. Opens circuit if failure rate exceeds 50%
3. Waits 30 seconds before attempting recovery
4. Prevents cascading failures to downstream services

### Retry Configuration

```properties
resilience4j.retry.instances.aviationApi.max-attempts=3
resilience4j.retry.instances.aviationApi.wait-duration=1s
resilience4j.retry.instances.aviationApi.exponential-backoff-multiplier=2
```

**How it works:**
1. Retries failed requests up to 3 times
2. Uses exponential backoff: 1s, 2s, 4s
3. Only retries on recoverable errors (503, timeouts, connection errors)
4. Does not retry on client errors (404, 400)

### Rate Limiter Configuration

```properties
resilience4j.ratelimiter.instances.aviationApi.limit-for-period=50
resilience4j.ratelimiter.instances.aviationApi.limit-refresh-period=1s
```

**How it works:**
1. Limits to 50 requests per second
2. Protects against excessive API usage
3. Returns 429 Too Many Requests when exceeded

## Observability

### Health Endpoints

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Circuit breaker health
curl http://localhost:8080/actuator/health/circuitbreakers

# Rate limiter health
curl http://localhost:8080/actuator/health/ratelimiters
```

### Metrics

```bash
# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# All metrics
curl http://localhost:8080/actuator/metrics

# Specific metric
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls
```

### Logging

Application uses structured logging with different levels:
- **DEBUG**: Application-specific logs (com.sporty.behometask)
- **INFO**: Framework logs
- **ERROR**: Error conditions with stack traces

Logs include:
- Request/response information
- External API calls
- Retry attempts
- Circuit breaker state changes
- Error conditions with trace IDs

## Architecture Decisions

### 1. Provider Abstraction Pattern

**Decision**: Created `AviationDataClient` interface with provider-specific implementation.

**Rationale**: 
- Decouples service layer from specific aviation API provider
- Allows switching providers without changing business logic
- Facilitates testing with mock implementations
- Follows Dependency Inversion Principle

**Trade-offs**: 
- Additional abstraction layer adds slight complexity
- Worth it for long-term maintainability and flexibility

### 2. Resilience4j for Resilience Patterns

**Decision**: Used Resilience4j library for circuit breaker, retry, and rate limiting.

**Rationale**:
- Battle-tested library designed for Spring Boot
- Declarative approach using annotations
- Built-in metrics and monitoring
- Comprehensive configuration options
- Lightweight and performant

**Alternatives Considered**:
- Netflix Hystrix (deprecated)
- Custom implementation (reinventing the wheel)

### 3. RestTemplate over WebClient

**Decision**: Used RestTemplate for HTTP client.

**Rationale**:
- Simpler synchronous model suitable for this use case
- Well-integrated with Resilience4j
- Adequate performance for current requirements
- Easier to understand and maintain

**Future Consideration**: 
- Could migrate to WebClient for reactive/non-blocking if high concurrency needed

### 4. Validation at Service Layer

**Decision**: ICAO code validation performed in service layer, not controller.

**Rationale**:
- Business logic belongs in service layer
- Keeps controller thin and focused on HTTP concerns
- Validation reusable across different entry points (REST, messaging, etc.)
- Easier to test business rules

### 5. Provider-Specific DTOs

**Decision**: Separate DTOs for external API (`AviationApiAirportResponse`) and public API (`AirportResponse`).

**Rationale**:
- Isolates internal model from external API changes
- Public API stability independent of provider
- Easier to add/change providers
- Clear mapping between external and internal models

### 6. Global Exception Handler

**Decision**: Centralized exception handling using `@RestControllerAdvice`.

**Rationale**:
- Consistent error response format across all endpoints
- Single place to manage error-to-HTTP status mapping
- Automatic trace ID generation for debugging
- Prevents exception details leakage to clients

### 7. Spring Boot Actuator for Observability

**Decision**: Enabled Actuator with health, metrics, and Prometheus endpoints.

**Rationale**:
- Production-standard observability out of the box
- Prometheus integration for metrics collection
- Health checks for Kubernetes/cloud deployments
- Minimal configuration required

## Error Handling

### Error Response Format

All errors return a standardized `ErrorResponse`:

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "User-friendly error message",
  "path": "/api/v1/airports/ABC",
  "traceId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Exception Hierarchy

1. **InvalidIcaoCodeException** → 400 Bad Request
2. **AirportNotFoundException** → 404 Not Found
3. **AviationApiException** → 503 Service Unavailable
4. **CallNotPermittedException** (Circuit Breaker) → 503 Service Unavailable
5. **RequestNotPermitted** (Rate Limiter) → 429 Too Many Requests
6. **Generic Exception** → 500 Internal Server Error

### Trace IDs

Each error response includes a unique `traceId` (UUID) for:
- Correlating logs across services
- Debugging specific requests
- Customer support investigations

## Future Improvements

### Short-term Enhancements

1. **Caching**: Add Redis/Caffeine cache for frequently requested airports
   - Reduces external API calls
   - Improves response times
   - Implement with Spring Cache abstraction

2. **Request Logging**: Add request/response logging interceptor
   - Log all incoming requests
   - Log outgoing API calls
   - Useful for debugging and audit

3. **Response Compression**: Enable gzip compression
   - Reduces bandwidth usage
   - Improves response times for large payloads

4. **Security**: Add API authentication
   - API key authentication
   - Rate limiting per client
   - OAuth2 integration

### Medium-term Enhancements

1. **Multiple Provider Support**: Implement fallback to secondary providers
   - Primary provider fails → fallback to backup
   - Load balancing across providers
   - Cost optimization

2. **Async Processing**: Migrate to WebClient for reactive processing
   - Better scalability under high load
   - Non-blocking I/O
   - Backpressure handling

3. **Database Integration**: Store airport data locally
   - Cache frequently accessed airports
   - Reduce external API dependency
   - Enable offline mode

4. **Batch Operations**: Support multiple ICAO code lookups
   - Single request for multiple airports
   - Optimized for use cases needing multiple airports

### Long-term Enhancements

1. **Search Capabilities**: Search airports by name, city, country
   - More flexible than ICAO-only
   - Better user experience
   - Requires database or search engine

2. **GraphQL API**: Add GraphQL alongside REST
   - Clients fetch only needed fields
   - Reduces over-fetching
   - Better for mobile clients

3. **Event-Driven Architecture**: Emit events for airport lookups
   - Analytics and monitoring
   - Integration with other services
   - Kafka/RabbitMQ integration

4. **Multi-Region Deployment**: Deploy across multiple regions
   - Reduced latency
   - High availability
   - Disaster recovery

## AI-Generated Code Disclosure

This project was developed with assistance from AI tools (Claude by Anthropic). The AI was used to:

1. **Generate boilerplate code**: DTOs, configuration classes, standard Spring Boot setup
2. **Implement patterns**: Circuit breaker, retry, rate limiter configurations
3. **Create tests**: Unit test structure and integration test scenarios
4. **Documentation**: README structure and API documentation

**Human verification and adaptation**:
- All generated code was reviewed and adapted for requirements
- Architecture decisions made by human developer
- Test scenarios designed based on real-world failure modes
- Configuration values tuned for production use
- Code follows project conventions and best practices

**Not AI-generated**:
- Overall architecture and design decisions
- Specific business logic and validation rules
- Integration strategy and layering approach
- Trade-off analysis and future improvements
