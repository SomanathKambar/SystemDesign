# Requirements: Rate Limiter Library

## 1. Functional Requirements
- **FR1: Rate Decisioning**: The system must decide whether a request is allowed based on the user's key and configured limits.
- **FR2: Multiple Strategies**: Support for different algorithms:
    - **Fixed Window**: Simple, memory-efficient, allows bursts at window boundaries.
    - **Sliding Window**: Accurate, prevents boundary bursts, tracks sub-window intervals.
- **FR3: Retry-After Metadata**: Denied requests must include the duration (in ms) until the next window opens or a slot becomes available.
- **FR4: Persistence Abstraction**: The core logic must be decoupled from storage (In-memory, Redis, etc.).

## 2. Non-Functional Requirements
- **NFR1: Low Latency**: Rate limiting checks must complete in under 2ms for in-memory and under 10ms for distributed stores.
- **NFR2: High Throughput**: Must handle thousands of checks per second per node.
- **NFR3: Accuracy**: Sliding window implementation must be precise to the millisecond.
- **NFR4: Resiliency**: If the state store is unavailable, the system should default to "Fail-Open" (configurable) to avoid blocking legitimate traffic.
- **NFR5: Thread Safety**: Algorithm implementations must be thread-safe for concurrent request processing.
