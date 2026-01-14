# Rate Limiter Infrastructure Playground

## Overview
This application is a **System Design Infrastructure Playground** designed to visualize, test, and compare various rate-limiting strategies in real-time.

It is built with a **Strategy-First Architecture**, allowing engineers to toggle between different algorithms to observe how they handle traffic bursts, window boundaries, and load distribution.

## Core Features
- **Algorithm Agnostic**: Support for multiple strategies (Fixed Window, Sliding Window) through a unified interface.
- **Dynamic Configuration**: Hot-swap limits and window sizes without restarting the server.
- **Visual Analytics**: Real-time progress bars and strategy-specific metrics (e.g., Estimated Counts for Sliding Window).
## Extensibility
- Designed to easily accommodate future strategies like *Token Bucket* or *Leaky Bucket*.

## Test Scenarios & Expected Outcomes

### 1. Fixed Window Strategy
**Configuration**: Limit = 3, Window = 10,000ms (10s)

| Action | Time Offset | Expected Result | Reason |
| :--- | :--- | :--- | :--- |
| Click 1 | 0s | **Allowed** | Counter = 1 |
| Click 2 | 1s | **Allowed** | Counter = 2 |
| Click 3 | 2s | **Allowed** | Counter = 3 (Limit Reached) |
| Click 4 | 3s | **Blocked** | Counter > 3. `retryAfter` ~7s |
| Click 5 | 11s | **Allowed** | New Window started. Counter reset to 1. |

### 2. Sliding Window Strategy
**Configuration**: Limit = 5, Window = 10,000ms (10s)

*Context: Assumes the previous window was fully utilized (5 requests).*

| Action | Time Offset | Estimated Count | Expected Result |
| :--- | :--- | :--- | :--- |
| Click 1 | 1s | `0 + (5 * 0.9) = 4.5` | **Allowed** (4.5 < 5) |
| Click 2 | 2s | `1 + (5 * 0.8) = 5.0` | **Blocked** (5.0 >= 5) |
| Click 3 | 5s | `1 + (5 * 0.5) = 3.5` | **Allowed** (3.5 < 5) |
| Click 4 | 6s | `2 + (5 * 0.4) = 4.0` | **Allowed** (4.0 < 5) |

*Note: Sliding window prevents the "Double Burst" where a user could send 3 requests at the end of window A and 3 at the start of window B.*

## UI Flow Verification Guide

To verify the system end-to-end, follow these steps in the dashboard:

1.  **Selection**: Choose your strategy from the **Strategy Dropdown**.
2.  **Configuration**: Set the Limit and Window size, then click **"Update Configuration"**.
    - *Verification*: Look for the system log entry: `[Time] System: Config updated: STRATEGY | L req / W ms`.
3.  **Manual Pulse**: Click **"Send Single Request"**.
    - *Verification*:
        - The **Visualizer** should pulse Green (Allowed) or Red (Blocked).
        - The **Window Progress** bar should update its width.
        - The **Strategy Metrics** (Est. Count or Progress %) should update immediately.
4.  **Load Stress**: Click **"Start Load Test"**.
    - *Verification*:
        - Observe the pattern of successes and failures in the **Request Log**.
        - For **Sliding Window**, watch the `Est. Count` dynamically change as time passes, allowing requests even before a hard window reset.
        - For **Fixed Window**, watch the `Window Progress` hit 100% before the block is released.
5.  **Telemetry**: Check the **Statistics** boxes to ensure `Total Traffic`, `Allowed`, and `Blocked` counters increment correctly.

