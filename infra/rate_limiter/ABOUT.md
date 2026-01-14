# Fixed Window Rate Limiter - Interactive Demo

## Core Working Principles

The **Fixed Window** algorithm is one of the simplest and most common rate limiting strategies. It operates on the concept of discrete time buckets.

### 1. The Algorithm
- **Time Windows**: Time is divided into fixed intervals of length `windowSizeMs` (e.g., 10,000ms).
- **Bucketing**: Each window corresponds to a unique time bucket. For example, if the window size is 10s:
    - Bucket 1: `00:00:00` to `00:00:09`
    - Bucket 2: `00:00:10` to `00:00:19`
- **Counting**: When a request comes in, we calculate which bucket it belongs to based on the current server time. We then increment the counter for that specific bucket.
- **Decision**: 
    - If `Current Counter < Limit`: **Allow** the request.
    - If `Current Counter >= Limit`: **Block** the request (Drop it).

### 2. Configuration Dynamics: Limit vs. Window Size

The behavior of the limiter is defined by the relationship between the **Limit (L)** and the **Window Size (W)**.

#### The "Burst" Factor
Rate limiting is often about controlling burstiness versus average throughput.

*   **Scenario A: Long Window, Low Limit (Strict)**
    *   *Config*: Limit = 5, Window = 60,000ms (1 minute).
    *   *Behavior*: You can make 5 requests instantly. However, once you use them, you are **blocked for the remainder of the minute**.
    *   *Why it feels "broken"*: If you test this by clicking rapidly, you will succeed 5 times and then fail for a very long time (up to 59 seconds). This is intended behavior for preventing consistent load, but it punishes bursts heavily.

*   **Scenario B: Short Window, High Limit (Permissive)**
    *   *Config*: Limit = 5, Window = 1,000ms (1 second).
    *   *Behavior*: You can make 5 requests every second.
    *   *Observation*: If you click manually, it's hard to trigger a block because you might not click faster than 5 times a second. It feels "always working".

#### Proportion & Throughput
Even if the *ratio* is the same, the behavior differs:
*   `10 req / 10 sec` (Average: 1 req/sec) -> Allows a burst of 10 requests in the first second, then silence for 9 seconds.
*   `1 req / 1 sec` (Average: 1 req/sec) -> Smoother distribution. Rejects the 2nd request if it arrives within the same second.

## Application Architecture

This demo application bridges the backend logic with a reactive frontend to visualize these concepts.

### Backend (`app` module)
*   **Framework**: Ktor (Netty Engine).
*   **State Management**: Holds a singleton `RateLimiterManager`. When you change config in the UI, the manager swaps the underlying `FixedWindowRateLimiter` instance for a new one, effectively resetting the state.
*   **API**:
    *   `POST /api/request`: 
        1.  Calculates `windowStart` based on server time.
        2.  Checks the `InMemoryStateStore` for the key (e.g., "user1:17000050000").
        3.  Returns a `Decision` object containing `allowed` (boolean) and `retryAfterMs` (time until next window).

### Frontend (UI)
*   **Control Panel**: Sends `POST /api/config` to resize the window or change limits dynamically.
*   **Load Generator**: The "Start Load Test" button uses a JavaScript `setInterval` to send 5 requests/second.
    *   *Visualizing Limits*: If your limit is 10 req / 10 sec, and the load generator sends 5 req/sec:
        *   Sec 1: 5 requests (Total 5) -> **OK**
        *   Sec 2: 5 requests (Total 10) -> **OK**
        *   Sec 3: 5 requests (Total 15) -> **BLOCKED** (Limit exceeded)
        *   ... Blocked until Sec 11 (Next Window).
*   **Visualizer**:
    *   **Green Pulse**: CSS Animation triggered on HTTP 200 (Allowed).
    *   **Red Block**: CSS Class added on HTTP 200 (Blocked response), displaying the `retryAfter` countdown.

## Summary
The "failure" you see when increasing window size while keeping requests constant is the algorithm working correctly: you are extending the duration for which the counter persists, making it more likely for the accumulated requests to hit the ceiling.