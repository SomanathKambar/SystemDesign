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
- Implemented `SlidingWindowRateLimiter` logic:
    - Eviction of expired timestamps.
    - Log count check.
    - Atomic-like updates via thread-safe in-memory store.
- Implemented `InMemorySlidingWindowStore`.

### Phase 3: Documentation (Completed)
- Created `REQUIREMENTS.md`.
- Created `FAILURE_MODES.md`.
- Created `TRADEOFFS.md`.
- Created `docs/strategies/SLIDING_WINDOW.md` (LLD).
- Updated `ARCHITECTURE.md` (HLD).

### Phase 4: Verification (Completed)
- Unit tests for basic allowance.
- Unit tests for precise window sliding over time.

## 4. Next Steps
- Implement `Token Bucket` strategy.
- Implement `Redis` backing for all stores to support distributed environments.
- Add `Spring Boot Starter` or `Auto-Configuration` for easy integration into web projects.