# Strategy Trade-offs

## Comparison Matrix

| Feature | Fixed Window | Sliding Window (Log) | Sliding Window (Counter) | Token Bucket |
| :--- | :--- | :--- | :--- | :--- |
| **Accuracy** | Low (Boundary issues) | High (Precise) | Medium (Approximation) | High |
| **Memory Usage** | Very Low (1 counter/key) | High (N timestamps/key) | Low (2 counters/key) | Low (1 counter + 1 time) |
| **Complexity** | Simple | Moderate | Complex | Moderate |
| **Burst Handling** | Allows 2x limit at edges | Strict | Smooth | Allows bursts up to bucket size |

## Selection Guide

1. **Use Fixed Window when**:
    - Memory is constrained.
    - Absolute precision at the second/minute boundary is not critical.
    - Throughput is extremely high and latency must be minimal.

2. **Use Sliding Window (Log) when**:
    - Strict adherence to the limit over any time interval is required.
    - The number of requests per window is relatively small (to save memory).
    - Preventing "boundary spikes" is essential.

3. **Use Token Bucket when**:
    - You want to support occasional bursts of traffic but keep the *average* rate constant.
    - You are modeling resource consumption (e.g., bytes/sec) rather than just request counts.
