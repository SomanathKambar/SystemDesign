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



# Rate Limiter Lab — Execution Plan (Authoritative)

This document is the **single source of truth** for planning, execution,
and progress tracking of the Rate Limiter Lab project.

Any LLM or contributor MUST:
- Read this file first
- Update this file after completing steps
- Never restart design from scratch
- Always resume from the latest checkpoint

---

## 0. Project Intent

Build a **static-first, deterministic, visual system design lab** that:

- Demonstrates rate limiting strategies (Token Bucket, Fixed Window, Sliding Window)
- Uses **offline-generated event logs**
- Replays them with **rich animations**
- Highlights trade-offs under different traffic profiles
- Is hosted on **GitHub Pages**
- Is SEO- and blog-friendly

Primary audience:
- Senior engineers
- System design learners
- Interview preparation
- Technical blogging

---

## 1. Core Architectural Principles (Non-Negotiable)

1. **Deterministic Simulation**
  - No randomness at render time
  - All behavior comes from precomputed events
  - Uses a `Clock` abstraction for simulated time

2. **Strict Separation**
  - Engine (logic) ≠ Visualization (UI)
  - No rate limiting logic in frontend

3. **Static-First**
  - No backend required in V1
  - All data served as static JSON / JSONL

4. **Replay & Time Travel**
  - Play / Pause / Seek / Step supported
  - Entire experiment must be replayable

5. **Gradual Enhancement**
  - Existing package structure (`com.systemdesign.infra.ratelimiter`) reused
  - New modules added incrementally
  - No rewrites

---

## 2. Final Tech Stack (V1)

### Frontend
- React
- JavaScript / TypeScript
- Vite
- Canvas + SVG (hybrid)
- Hosted on GitHub Pages

### Simulation / Logic (The "Engine")
- Kotlin (existing logic in `core` and `strategies`)
- Gradle multi-module
- CLI-based offline runner (`runner` module)

### Data
- JSON (configs, metrics)
- JSONL (event streams)

---

## 3. Repository Layout (Target State)

The project is located within the `SystemDesign` monorepo at `infra/rate_limiter/`.

```
SystemDesign/
└── infra/
    └── rate_limiter/
        ├── core/                  # Shared interfaces, models, and Event definitions
        ├── strategies/            # Algorithm implementations (Fixed, Sliding, Token Bucket)
        ├── persistence/           # Storage implementations (Redis, etc.)
        ├── starters/              # Spring Boot integration
        ├── app/                   # Existing Ktor-based visualization playground
        ├── runner/                # NEW: Offline simulation runner (CLI)
        ├── experiments/           # NEW: Generated JSON/JSONL artifacts (git-ignored logs)
        ├── frontend-lab/          # NEW: React + Vite lab (for GitHub Pages)
        ├── docs/                  # Architecture and strategy documentation
        └── plan.md                # THIS FILE
```

---

## 4. Canonical Event Model (Locked)

Events are **facts**, not interpretations. Defined in `com.systemdesign.infra.ratelimiter.core.event`.

Every event MUST have:

```json
{
  "eventId": "uuid",
  "timestampMs": 123,
  "strategy": "TOKEN_BUCKET",
  "nodeId": "node-1",
  "type": "REQUEST_ALLOWED",
  "payload": {}
}
```

Event categories:
- Request lifecycle (ALLOWED, BLOCKED)
- Strategy-specific (TOKEN_REFILLED, WINDOW_SHIFTED)
- Time control (TICK)
- Failure injection (NODE_DOWN)

---

## 5. Execution Phases

### PHASE 0 — Reusable Shared Code & Foundation (COMPLETED)
- [x] Define `Clock` interface in `core` for deterministic time.
- [x] Define `RateLimitEvent` and sub-types in `core`.
- [x] Create `EventEmittingRateLimiter` wrapper in `core` to capture events during simulation.
- [x] Implement `TestClock` for controlled time advancement.
- [x] Refactor existing strategies to use custom `Clock`.

### PHASE 1 — Engine Finalization (COMPLETED)
- [x] Normalize event schema across all strategies (deep integration for internal events).
- [x] Create `runner` module for offline simulation.
- [x] Add traffic generator (Burst, Constant, Random) in `runner`.
- [x] Implement `Simulator` to produce deterministic event logs.

### PHASE 2 — Offline Runner & Experiment Generation (COMPLETED)
- [x] Implement `ExperimentWriter` to save structured bundles (config + logs) to `experiments/`.
- [x] Generate canonical experiment set for all strategies (Token Bucket, Fixed, Sliding Counter/Log).
- [x] Verify generated artifacts against `Canonical Event Model`.

### PHASE 3 — Frontend Scaffold (COMPLETED)
- [x] Initialize `frontend-lab` (Vite + React + Tailwind).
- [x] Implement `ExperimentLoader` (fetch JSON/JSONL from `experiments/`).
- [x] Build Timeline controller (Play/Pause/Seek).
- [x] Scaffold Canvas-based visualization layer.

### PHASE 4 — Strategy Visualizations (COMPLETED)
- [x] Implement `TokenBucketVisualizer` (Canvas bucket metaphor).
- [x] Implement `FixedWindowVisualizer` (Grid/Counter display).
- [x] Implement `SlidingWindowVisualizer` (Moving log/timeline for both Counter and Log).
- [x] Connect Event stream to visual intents.
- [x] Implement Strategy Selector in UI.

### PHASE 5 — Comparison & Deployment (IN PROGRESS)
- [x] Implement Comparison Mode (side-by-side playback).
- [x] Basic winner detection logic.
- [ ] UI Refinement: Fix block visibility in comparison view.
- [x] Simulation Refinement:
    - **FIXED:** Simulator now emits granular TICK events every 100ms.
    - **FIXED:** Token Bucket interpolates refill on the frontend for smooth visualization.
    - **FIXED:** Visualizers use config windowSizeMs instead of hardcoded values.
    - **FIXED:** High load profiles (12 RPS) correctly demonstrate Fixed Window's blocking behavior.
- [x] Data Refinement: Generate better traffic profiles (Upper/Lower/Mid boundaries) to demonstrate strategy differences.
- [ ] Setup GitHub Actions for automated deployment to GitHub Pages.

## CURRENT STATE
Phase: PHASE 5
Last completed step: Side-by-side comparison implemented.
Next step: Refactor Simulator to support granular time stepping and TICK events.
Interruption Snapshot: 
- Identified that Simulator jumps time too fast and doesn't emit intermediate state events.
- Token Bucket refill only happens during `allow()` calls, making it look broken in UI when no requests are made.
Open questions: None.


## 8. Verification & Correctness Audit

### 1. Token Bucket: The "Flexible" Metric
- **Target Logic**: `Allow if tokens >= 1.0`.
- **Refill Algorithm**: Lazy calculation: `newTokens = min(capacity, currentTokens + (now - lastRefill) * refillRate)`.
- **Precision**: Uses `Double` for tokens to ensure smooth refill even at high frequencies.
- **Wait Time**: `(1.0 - currentTokens) / refillRate` converted to milliseconds.
- **Trade-off**: High precision and burst support, but slightly more complex than counters.

### 2. Leaky Bucket: The "Smoothing" Metric
- **Target Logic**: `Allow if bucket has space`.
- **Drip Algorithm**: Constant outflow rate.
- **Implementation**: Can be implemented using a "Next Available Time" slot or a FIFO queue.
- **Difference from Token Bucket**: Token Bucket allows bursts up to capacity; Leaky Bucket forces a steady stream (shaping).
- **Status**: **PENDING IMPLEMENTATION**.

### 3. Fixed Window Counter: The "Cheap" Metric
- **Target Logic**: `Allow if counter < limit`.
- **Reset Logic**: `windowStart = (now / windowSize) * windowSize`.
- **Critical Flaw**: Window boundary burst (2x limit in short duration).
- **Trade-off**: Extremely low memory (1 counter per key) and high performance.

### 4. Sliding Window Counter: The "Precision Estimate"
- **Target Logic**: `weightedSum = currentCount + (previousCount * weight)`.
- **Weight**: `(windowSize - timeElapsedInCurrentWindow) / windowSize`.
- **Accuracy**: Good approximation of a sliding window without the memory overhead of a log.
- **Trade-off**: Much better than Fixed Window at boundaries, still uses constant memory.

### 5. Sliding Window Log: The "Perfect" Metric
- **Target Logic**: `count(timestamps in [now - windowSize, now]) < limit`.
- **Implementation**: Store timestamps in a list/ZSET and filter old ones on every request.
- **Accuracy**: 100% accurate sliding window.
- **Trade-off**: Memory scales with the limit (O(N)).

---

## 9. Execution Plan (Refined)

### Step 1: Implement Leaky Bucket
- [ ] Create `LeakyBucketRateLimiter` in `:strategies:leaky-bucket`.
- [ ] Implement `InMemoryLeakyBucketStore`.
- [ ] Add unit tests verifying the "smoothing" behavior (constant outflow).

### Step 2: Correctness & Concurrency Audit
- [ ] **Boundary Test**: Verify Fixed Window "double burst" and how Sliding Window Counter mitigates it.
- [ ] **Concurrency Test**: Ensure `store.compute` handles high-contention updates without losing tokens/counts.
- [ ] **Precision Test**: Compare `SlidingWindowLog` vs `SlidingWindowCounter` under jittery traffic.

### Step 3: Visualization & Lab Update
- [ ] Add `LEAKY_BUCKET` to `frontend-lab`.
- [ ] Update `runner` to generate `LEAKY_BUCKET` experiments.
- [ ] Fix comparison view visibility issues.

### Step 4: Distributed Readiness
- [ ] Verify `persistence:redis` implementations for all strategies.
- [ ] Ensure Lua scripts are used (or planned) for Redis atomicity if multi-key operations are needed.

---

## CURRENT STATE
Phase: PHASE 5 (Verification & Refinement)
Last completed step: Fixed UI data loading issues. Deterministic experiment IDs implemented.
Next step: Distributed Readiness - Verify Redis implementations and ensure Lua scripts for atomicity.
Interruption Snapshot: 
- Fixed `runner` path issues to correctly output experiments to `frontend-lab/public/experiments`.
- Implemented deterministic experiment IDs (e.g., `leaky_bucket_burst`) to stabilize frontend fetching.
- Added `LEAK_OCCURRED` event to `core` for precise Leaky Bucket visualization.
- Improved `ComparisonDashboard` robustness with `Promise.allSettled`-like logic and `try-catch`.
- Fixed potential canvas API compatibility issues (replaced `roundRect` with `rect`).
Open questions: None.

## Verify 

---

# ✅ PART 2 — MASTER PROMPT (PASTE THIS EVERY TIME)

### 🧠 MASTER PROMPT — *Rate Limiter Lab Executor*

You are an execution assistant for the project "Rate Limiter Lab" inside `SystemDesign/infra/rate_limiter`.

Rules you MUST follow:
1. **Read `plan.md`** first. It is the single source of truth.
2. **Resume** from the "Next step" in the "CURRENT STATE" section.
3. **Token Management & State Persistence**: 
   - If tokens are nearing exhaustion or the task is too large, **prioritize updating `plan.md`** first.
   - Skip further code generation and document exactly what was skipped in the "Open questions" or "Pending" section.
   - Ensure the snapshot is detailed enough for a fresh LLM instance to resume without context loss.
4. **Use Packages**: `com.systemdesign.infra.ratelimiter.*`.
5. **Deterministic Time**: Always use the `Clock` abstraction.
6. **Small Steps**: Work incrementally and update `plan.md` after each milestone.

Your output must be concrete, executable, and aligned with the existing structure.
Never start from scratch. Always resume.


