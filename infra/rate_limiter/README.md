# Rate Limiter Library

## Overview
This is a modular, high-performance Rate Limiter library designed for distributed systems. It follows a "plugin" architecture where the core logic is decoupled from specific algorithms (strategies) and state storage mechanisms.

## Project Structure
The project is organized as a Gradle multi-module build:

- **`core`**: Contains the primary interfaces (`RateLimiter`, `StateStore`) and domain models (`Decision`, `CounterState`).
- **`strategies`**:
  - **`fixed-window`**: Implementation of the Fixed Window Counter algorithm.

## Getting Started

### Prerequisites
- Java 17+
- Gradle (Wrapper included)

### Building
```bash
./gradlew build
```

### Testing
```bash
./gradlew test
```

### Usage Example
```kotlin
val rateLimiter = FixedWindowRateLimiter(
    limit = 100,
    windowSizeMs = 60_000, // 1 minute
    stateStore = InMemoryStateStore() // Or RedisStateStore()
)

val decision = rateLimiter.allow("user_123")
if (decision.allowed) {
    // Process request
} else {
    // Return 429 Too Many Requests
    // Retry after: decision.retryAfterMs
}
```

## Interactive Demo Application
This project includes a standalone Web Application to visualize and test the Fixed Window Rate Limiter.

### Running the App
```bash
./gradlew :app:run
```
*Note: This starts a Ktor server on http://0.0.0.0:8080*

### Features
1.  **Configuration**: Dynamically change `limit` and `windowSize` via the UI.
2.  **Visual Feedback**: See requests being allowed (Green) or blocked (Red) in real-time.
3.  **Load Testing**: Simulate traffic (5 req/sec) to test the limiter's behavior under load.
4.  **Logging**: Detailed log of accepted and rejected requests with timestamps.

Access the UI at: [http://localhost:8080](http://localhost:8080)
