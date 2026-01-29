# Implementation Plan: Sliding Window Rate Limiter

## 1. Requirement Analysis
The goal is to provide a "Smooth" rate limiter that prevents the burst issues of Fixed Window. This requires sub-window accuracy.

### Scope
- Implement `SlidingWindowLog` algorithm.
- Provide a thread-safe in-memory store.
- Comprehensive testing for edge cases (millisecond boundary shifts).

## 2. Design Phase
- **Algorithm**: `O(N)` space complexity where N is the limit.
- **Accuracy**: 100% accurate sliding window.
- **Storage**: Decoupled via `SlidingWindowStore` interface to allow future Redis implementation using Sorted Sets (ZSET).

## 3. Implementation Steps

### Phase 1: Core Evolution (Completed)
- Added `SlidingWindowLog` model to `core`.
- Added `SlidingWindowStore` interface to `core`.

### Phase 2: Implementation (Completed)
- Created `:strategies:sliding-window` module.
- Implemented `SlidingWindowRateLimiter` (Counter) and `SlidingWindowLogRateLimiter` (Log) logic.
- Implemented `InMemorySlidingWindowStore`.
- Created `:strategies:token-bucket` module.
- Implemented `TokenBucketRateLimiter` logic with atomic `compute`.
- Implemented `InMemoryTokenBucketStore`.
- Created `:persistence:redis` module.
- Implemented `RedisStateStore`, `RedisTokenBucketStore`, and `RedisSlidingWindowStore` using Jedis.
- Fixed race conditions in all strategies by adding `compute` method to store interfaces.
- Created `:starters:spring-boot-starter-ratelimiter` module.
- Implemented `RateLimiterAutoConfiguration` and `RateLimiterProperties`.

### Phase 3: Documentation (Completed)
- Created `REQUIREMENTS.md`.
- Created `FAILURE_MODES.md`.
- Created `TRADEOFFS.md`.
- Created `docs/strategies/SLIDING_WINDOW.md` (LLD).
- Created `docs/strategies/TOKEN_BUCKET.md` (LLD).
- Updated `ARCHITECTURE.md` (HLD).

### Phase 4: Verification (Completed)
- Unit tests for Sliding Window.
- Unit tests for Token Bucket (including concurrency checks).
- Compilation and build of all modules including Redis persistence and Spring Boot Starter.

## 5. Phase 5: Visualization & Benchmarking (Completed)
- **Frontend Update**:
    - Updated `index.html` to include `TOKEN_BUCKET`, `SLIDING_WINDOW_COUNTER`, and `SLIDING_WINDOW_LOG` in the strategy selection.
    - Added specific UI controls for Token Bucket (capacity, refill rate).
    - Enhanced benchmarking dashboard to compare all 4 strategies simultaneously with a line chart.
    - Added real-time metric display for "Estimated Count" and "Available Tokens".
- **Backend Update**:
    - Updated `App.kt` to support all strategy types in the `RateLimiterManager`.
    - Extended `runComparison` logic to include all 4 strategies in a simultaneous stress test simulation.
    - Captures strategy-specific metadata (tokens, estimated counts) for the UI.

## 6. Benchmarking Suite (Future)
- Create a standalone benchmark module using JMH (Java Microbenchmark Harness) to measure latency and throughput of each strategy/store combination (e.g., Token Bucket + Redis vs. Fixed Window + In-Memory).

## 7. Conclusion
The Rate Limiter Infrastructure is now complete with support for multiple strategies (Fixed Window, Sliding Window Counter/Log, Token Bucket), distributed environments (Redis), easy Spring Boot integration, and a comprehensive visualization playground.