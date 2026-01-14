# Failure Modes & Mitigation

| Failure Scenario | Impact | Mitigation Strategy |
| :--- | :--- | :--- |
| **State Store Latency** | Increases API response time. | Implement timeouts for store access. Default to "Fail-Open" if timeout is exceeded. |
| **State Store Downtime** | Rate limiting is bypassed or all requests are blocked. | Circuit breaker pattern. Fail-open by default to ensure availability over strict throttling. |
| **Memory Exhaustion (Sliding Window)** | `OutOfMemoryError` due to storing too many timestamps. | Implement maximum log size per key. Use a store with automatic LRU eviction (like Redis). |
| **Clock Drift** | Inaccurate window calculations across distributed nodes. | Use a centralized time source (e.g., Redis `TIME`) or ensure NTP synchronization on all app servers. |
| **Race Conditions** | Slight over-allowance of requests (e.g., 102 requests allowed instead of 100). | Acceptable for most use cases. For strict limits, use atomic operations (Redis Lua scripts) or distributed locks. |
| **Hot Keys** | Single user/key overwhelming the state store node. | Implement local "L1" cache for frequent keys or use consistent hashing for store partitioning. |
