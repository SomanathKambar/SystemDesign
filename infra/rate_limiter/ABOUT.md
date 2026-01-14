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

## Sliding Window Counter Algorithm



While Fixed Window is simple, it can allow a "double burst" at window boundaries. The **Sliding Window Counter** approximates a smoother limit.



### How it works:

- It looks at the count in the **Current Window** and the **Previous Window**.

- It calculates a weighted sum: 

  `Estimated Count = Current Window Count + (Previous Window Count * (1 - Percentage of Current Window Elapsed))`

- **Benefit**: It smooths out the boundary issues of Fixed Window without the storage overhead of a full "Sliding Window Log" (which stores every timestamp).



### Proportion & Throughput

- If you are 30% into your 10-second window, and you had 10 requests in the previous 10 seconds, the algorithm assumes `10 * (1 - 0.3) = 7` requests from the "sliding" portion of the previous window still count toward your current limit.
