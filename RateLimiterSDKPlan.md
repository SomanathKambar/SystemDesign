# Policy-Driven Rate Limiter SDK & Learning Lab: Master Execution Plan

> **Status**: Active
> **Current Snapshot**: SNAPSHOT_V3 (Phases 1-4 Complete)
> **Role**: Staff Engineer / Architect
> **Objective**: Build a production-grade, policy-driven decision engine that bridges the gap between "learning/experimentation" and "governed/safe production" environments.

---

## 1. Vision & Core Philosophy

### 1.1. Identity
This is **not** an LLM or a generic rule engine. It is a **Policy-Driven Runtime Intelligence** SDK.
It answers the question: *"Why did the system make this decision?"*
It solves **Decision Opacity** in production systems.

### 1.2. The "Same Same But Different" Model
We utilize a single shared engine with two distinct "Capability Profiles":

1.  **Simulation Mode (Explorer Profile)**:
    *   **Audience**: Learners, System Design Students, Hustlers.
    *   **Behavior**: High configurability, "Break Glass" allowed.
    *   **Feedback**: Warnings for unsafe configs (e.g., "High burst risk"), but execution is permitted.
    *   **Goal**: "Let me touch the fire, but show me the burn."

2.  **Operational Mode (Governed Profile)**:
    *   **Audience**: SREs, Backend Engineers, Production Systems.
    *   **Behavior**: Strict constraints, Safe Operating Envelopes enforced.
    *   **Feedback**: Hard Rejects for unsafe configs (e.g., "Window < 1s rejected").
    *   **Goal**: "Protect me from myself."

### 1.3. Core Mandates
*   **CPU-First**: No GPU dependencies. Efficient, lightweight.
*   **Deterministic**: Same input + Same Policy = Same Decision.
*   **Immutable Strategies**: Strategies are stateless execution units.
*   **Patterns > Config**: Users think in patterns (Fallback, Throttling), not class names.
*   **Scars on Display**: The system explicitly exposes trade-offs (latency vs. accuracy).

---

## 2. Terminology & Concepts

To ensure professional "Scientific/Engineering" credibility, we map concepts as follows:

| Internal Concept | Public Terminology (Scientific/Engineering) |
| :--- | :--- |
| **Engine** | **Control Plane** |
| **Policy** | **Control Law** (Defines system response) |
| **Strategy** | **Mechanism** (How force is applied) |
| **Explorer Mode** | **Simulation Mode** |
| **Governed Mode** | **Operational Mode** |
| **Validation** | **Stability Analysis** |
| **Limits** | **Safe Operating Envelope** |
| **Decision Trace** | **Execution Trace** |
| **Replay** | **Simulation Replay** |

---

## 3. Architecture Overview

### 3.1. High-Level Flow
```
Request Context 
  (Inputs: Headers, Metrics, System Health)
       ⬇
[ Facade (DecisionClient) ]
       ⬇
[ Adapter Layer (Optional) ]
       ⬇
[ Control Plane (Decision Engine) ]
       ⬇
   [ Policy Router ] ➜ Selects Policy based on "Control Law"
       ⬇
   [ Capability Validator ] ➜ Checks "Safe Operating Envelope" (Sim vs Op)
       ⬇
   [ Strategy Factory ] ➜ Instantiates "Mechanism"
       ⬇
[ Strategy Execution ] (FixedWindow, TokenBucket, etc.)
       ⬇
[ Result + Execution Trace ]
```

### 3.2. Key Components
*   **Policy DSL**: YAML-based, declarative. No loops, no scripting.
    *   *Primitives*: Inputs, Conditions (When), Actions (Then).
*   **Capability Profiles**: The Strategy Pattern applied to validation.
    *   `ExplorerValidator`: Permissive.
    *   `GovernedValidator`: Strict.
*   **Strategy Taxonomy**:
    *   *Decision*: Allow, Deny, Throttle, Shed Load.
    *   *Control*: Fallback, Circuit Break.

---

## 4. Execution Plan (Phases)

### Phase 1: The Physics (Core Strategies)
**Goal**: Build the deterministic mechanisms (strategies) that enforce limits.
*   [x] **Define `Mechanism` Interface**: Stateless, context-aware.
*   [x] **Implement Core Algorithms**:
    *   `FixedWindowMechanism`
    *   `SlidingWindowMechanism`
    *   `TokenBucketMechanism`
    *   `LeakyBucketMechanism`
*   [x] **Unit Tests**: Verify mathematical correctness and edge cases.

### Phase 2: The Brain (Control Plane & Policy DSL)
**Goal**: Build the engine that parses rules and selects strategies.
*   [x] **Define DSL Schema**: Strict YAML/JSON schema for "Control Laws".
*   [x] **Implement Parser**: Load, validate structure, and cache policies.
*   [x] **Implement Policy Router**: Chain of Responsibility to match `Request` -> `Policy`.
*   [x] **Context Propagation**: Ensure `Inputs` (metrics, system health) are available to policies.

### Phase 3: The Guardrails (Capability System)
**Goal**: Implement the "Same Same But Different" logic.
*   [x] **Define `SafeOperatingEnvelope`**: Configurable limits for each strategy.
*   [x] **Implement `CapabilityProfile` Interface**.
*   [x] **Implement `SimulationValidator`**: Logs warnings, allows unsafe configs.
*   [x] **Implement `OperationalValidator`**: Throws `InstabilityRiskException`, rejects unsafe configs.
*   [x] **Validation Reporting**: Structured error responses.

### Phase 4: The Interface (Public SDK & Adapters)
**Goal**: Make it usable for developers.
*   [x] **Design `DecisionClient` Facade**: Simple, fluent API.
*   [x] **Implement Spring Boot Starter**: Auto-configuration.
*   [x] **Implement "Shadow Mode"**: Run engine without enforcing decisions (logging only).

### Phase 5: The Lab (Frontend & Visualization)
**Goal**: The "Explorer" experience.

**Pre-requisites**:
*   [x] **Stability Lock**: Ensure changes to `:app` and `:runner` do not break the data contracts used by the GitHub Pages hosted frontend.
*   [x] **Backward Compatibility**: The new `ExecutionTrace` format must be compatible with or transformable to the legacy event format.

**Tasks**:
*   [x] **Build React UI**:
    *   [x] Configuration Editors (Sliders for Simulation Mode).
    *   [x] Real-time Graphs ("Oscillation Visualization").
*   [x] **Integrate WebSockets/SSE**: Stream `ExecutionTrace` to the frontend.
*   [x] **"Break Glass" UI**: Explicit toggle to enable/disable safety limits.

### Phase 6: Graduation & Quality
**Goal**: Polish and Documentation.
*   [x] **Write "Graduation Guide"**: How to move from Simulation -> Operational.
*   [x] **Performance Benchmarking**: Ensure < 5ms overhead.
*   [x] **CI/CD Pipeline**: Automated testing for both profiles.
*   [x] **Release**: Publish artifacts (Configured).

---

## 5. Decision Log (Rationale)

*   **Why CPU-Only?** To ensure accessibility and sustainability. No hidden cloud costs.
*   **Why Two Profiles?** To serve both the "learning" use case (high engagement) and the "enterprise" use case (high trust) without forking the codebase.
*   **Why No ML Training?** We are a *Decision* engine, not a *Training* platform. We can plug into ML models, but we don't train them.

---

## 6. Next Actions
1.  Initialize **Phase 5** implementation: Integrate `DecisionClient` into the Learning Lab (Ktor App).
2.  Implement `ExecutionTrace` streaming for real-time visualization.
3.  Draft "Graduation Guide" for documentation.
