# Rate Limiter Application Architecture

## 1. Communication Pattern: Polling & Reactive UI

The application utilizes a **Client-Side Triggered Reactive Pattern** to simulate "real-time" updates.

### How it works:
- **Stateful Backend**: The Ktor server maintains a singleton `RateLimiterManager` which holds the current configuration and the in-memory state of the rate limiter.
- **Asynchronous Execution**: The UI does not use WebSockets (to keep the infra simple), but instead uses a **High-Frequency Request Loop** during load tests.
- **Immediate Feedback Loop**:
    1.  The UI (Client) sends a `POST /api/request`.
    2.  The Backend processes this against the `FixedWindowRateLimiter` logic instantly.
    3.  The response includes not just `allowed: true/false`, but also metadata: `retryAfterMs`, `currentLimit`, and `currentWindowSizeMs`.
    4.  The Frontend immediately reacts to this metadata to update the CSS-based visualizer and the log.

### UI Pattern: Pulse-Response
We use a **Pulse-Response** pattern for the visualization:
- Every request triggers a CSS "reflow" animation.
- Success results in a **Green Pulse**.
- Failure results in a **Red Block state** with an overlay showing the calculated `retryAfter` time, which is derived directly from the algorithm's window logic.

---

## 2. Technical Stack & Dependencies

### Backend (Kotlin/JVM)
1.  **Ktor (Server-Netty)**: 
    - *Why*: Ktor is an asynchronous framework built on Coroutines. It's lightweight and perfect for high-throughput microservices like a rate limiter.
    - *Role*: Handles the HTTP layer and routing.
2.  **Kotlinx Serialization**:
    - *Why*: It's the official Kotlin multiplatform serialization library.
    - *Role*: Converts Kotlin Data Classes (`ConfigRequest`, `RateLimitResponse`) to JSON for the UI.
3.  **Logback**:
    - *Role*: Provides structured logging for server-side monitoring.
4.  **AtomicReference (Java Standard Lib)**:
    - *Why*: Ensures thread-safety when updating the Rate Limiter configuration.
    - *Role*: Allows the `RateLimiterManager` to swap the entire algorithm configuration without stopping the server or causing race conditions during high load.

### Frontend (Vanilla JS/CSS)
- **Vanilla JavaScript**: 
    - *Why*: Zero build tools required (no React/Vue overhead), making the demo "run-and-play" ready.
- **Fetch API**: Uses standard browser APIs for asynchronous communication with the Ktor backend.

---

## 3. Interaction Diagram

```text
[ User UI ] <---- (JSON) ----> [ Ktor API ] <----> [ RateLimiterManager ]
    |                                                    |
    | (1) Set Config  -----------> [ Update Logic ] -----| (Atomic Swap)
    |                                                    |
    | (2) Send Request ----------> [ Check Logic ]  -----| (Lookup windowKey)
    |                                                    |
    | (3) Visual Update <--------- [ Response ] ---------| (Decision object)
```

## 4. Key Components Interaction

### `RateLimiterManager` (The Orchestrator)
This is the bridge between the web layer and the core logic. 
- When `updateConfig` is called, it creates a **new instance** of `FixedWindowRateLimiter`. 
- Since we use `InMemoryStateStore` by default, updating the config resets the counters. This allows users to "start fresh" when testing different throughput scenarios.

### `FixedWindowRateLimiter` (The Engine)
Located in the `:strategies:fixed-window` module. It is purely mathematical. It doesn't know about HTTP or JSON. It only knows:
- `now` (current time).
- `windowSize` (bucket width).
- `limit` (max count).
It calculates the bucket key (e.g., `user1:1704123000`) and asks the `StateStore` for the current count.

---

## 5. Why this architecture?
1.  **Decoupling**: The core algorithm remains "pure" (no web dependencies). You could take the `core` and `strategies` modules and use them in a CLI, a Spring Boot app, or an Android app without changing a line of code.
2.  **Concurrency**: By using Netty and Kotlin Coroutines (via Ktor), the app can handle thousands of simulated requests per second on a single machine, allowing for realistic load testing.
3.  **Observability**: The UI log and stats panel provide a "white-box" view into how the algorithm handles the boundary between one time window and the next.
