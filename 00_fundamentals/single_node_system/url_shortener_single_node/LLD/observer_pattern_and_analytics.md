# Architectural Decision: The Observer Pattern for Analytics

## Context
We needed to add "Observability" (Click Counts, Popularity Tracking) to our Single Node System.
However, adding this logic directly to `UrlService` introduced significant drawbacks.

## The Problem: The "Write Tax"
In a read-heavy system (like a URL Shortener), 99% of requests are "Reads" (Resolving a URL).
If we add `clickCount++` inside the main flow:
1.  **Performance Hit:** Every "Read" becomes a "Database Write."
2.  **Locking:** The database must lock the row to update the counter. If a link goes viral (10k req/sec), these locks create a bottleneck.
3.  **Coupling:** The `UrlService` becomes responsible for business logic (redirect) AND analytics.

## The Solution: Observer Pattern (Spring Events)

We decoupled "Doing the work" from "Watching the work."

### 1. WHY (The Benefit)
*   **Separation of Concerns:** `UrlService` focuses on the Redirect. `AnalyticsListener` focuses on Stats.
*   **Future Proofing:** If we want to switch from storing stats in H2 to sending them to Google Analytics, Kafka, or a Log File, we change the **Listener**, not the Service.
*   **Performance (Potential):** Currently, the listener is synchronous (for correctness). But simply adding `@Async` to the listener would instantly make the API response faster, moving the "Write Tax" to a background thread.

### 2. WHEN (The Trigger)
*   **`UrlCreationRequestedEvent`:** Fired every time `shorten()` is called.
    *   *Insight:* Tells us "Viral Demand." If `google.com` is submitted 1,000 times, we know it's popular, even if we only stored it once (deduplication).
*   **`UrlAccessedEvent`:** Fired every time `resolve()` is called.
    *   *Insight:* Tells us "Click Traffic."

### 3. WHAT (The Implementation)
*   **Publisher:** `UrlService` injects `ApplicationEventPublisher`.
*   **Subscriber:** `AnalyticsListener` annotated with `@EventListener`.
*   **Payload:** Events carry immutable data (`shortCode`, `timestamp`, `hash`).

### 4. HOW (The Flow)
1.  User clicks `bit.ly/ABC`.
2.  `UrlService` looks up `ABC` -> `google.com`.
3.  `UrlService` fires `UrlAccessedEvent("ABC")`.
4.  `UrlService` returns `google.com` (Fast!).
5.  Spring intercepts the event.
6.  `AnalyticsListener` receives event -> Updates DB `clickCount++`.

---
## Trade-offs
*   **Cons:** We still hit the DB for the update (Single Node constraint).
*   **Pros:** The code is clean. The "Write Tax" is isolated. We are ready for Async/Queue-based scaling.
