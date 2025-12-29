# Single Node System
# What is a “Single Node System”?

## A single-node system means: The entire system runs on one server / one machine.

# Why we start with Single Node

## Because system design mastery goes like this:

### 1.Make it work
### 2.Make it correct
### 3.Make it fast
### 4.Make it scalable
### 5.Make it resilient


# 1. How many redirects can Single-Node handle?
## Client → API Server → Single DB
### Assume a modest machine:

| Resource  | Typical Value         | Estimated Capacity  | Notes / Impact                  |
| --------- | --------------------- | ------------------- | ------------------------------- |
| CPU Cores | 8 vCPU                | ~8,000 RPS          | Handles concurrent requests     |
| RAM       | 32 GB                 | ~95% cache hit rate | Hot URLs in memory              |
| Disk      | 1 TB NVMe SSD         | 300k IOPS           | Supports DB reads/writes        |
| Network   | 1 Gbps                | ~125 MB/sec         | Max outbound redirects          |
| DB        | Postgres/MySQL        | 8k RPS              | Primary data store              |
| Cache     | OS page cache / Redis | 4k–6k RPS           | Reduces DB load                 |
| OS Limits | 65k file descriptors  | —                   | Supports concurrent connections |



# Why this matters

## Now the table gives actionable numbers, not just resources.

## You can reason about when vertical scaling fails, when to add nodes, and how to predict capacity.

## This turns your design into production-ready planning, not just theory.

## Cost of 1 redirect

# For every click:

| Step                     | Avg Time  | Impact / Notes                        | Bottleneck / Optimization           |
| ------------------------ | --------- | ------------------------------------- | ----------------------------------- |
| Network                  | 3–10 ms   | Round-trip time from client to server | Reduce via CDN / edge caching       |
| HTTP Parsing             | 1–2 ms    | Minimal CPU cost                      | Keep server lightweight             |
| DB Index Lookup          | 1–3 ms    | Critical operation                    | Indexed short_code, in-memory cache |
| Disk Read (cold)         | 5–10 ms   | Only if cache miss                    | Add RAM cache, SSD/NVMe             |
| Serialization / Response | 1 ms      | Convert long_url to HTTP 302          | Minimal overhead                    |
| Total Latency            | ~10–25 ms | Per redirect end-to-end               | Target <50ms SLA                    |

# ≈ 10–25 ms per redirect
# Max sustainable RPS:
## 1 core ≈ 1000 req/sec
## 8 cores ≈ 8000 req/sec

# Single node tops out at ~8,000 redirects/sec 
## ≈ 700 million redirects/day

### That is the absolute upper bound before collapse.

# Why this collapses suddenly

| Resource  | Safe Limit | Hard Limit | What Breaks        | User Impact        |
| --------- | ---------- | ---------- | ------------------ | ------------------ |
| CPU       | 65%        | 90%        | Thread starvation  | Requests time out  |
| RAM       | 75%        | 95%        | OS starts swapping | Latency explodes   |
| Disk IOPS | 60%        | 95%        | DB stalls          | Redirects freeze   |
| Network   | 70%        | 100%       | Packet drops       | 502 / 504 errors   |
| Open FDs  | 40k        | 65k        | No new sockets     | Server unreachable |

# Why Collapse Is Sudden (The Physics)

## Because all these limits interact:

• High CPU → slow DB
• Slow DB → request pile-up
• Request pile-up → RAM fills
• RAM fills → OS swaps
• Swap → CPU spikes
• CPU spikes → thread starvation
→ Instant death spiral

### This is called Resource Saturation Cascade.


# It looks like this:
## Everything fine → 10% more traffic → Full outage in seconds

# This is Why Single-Node “Looks Cheap But Is Dangerous”
## Because:
| Traffic | Single Node |
| ------- | ----------- |
| 10k RPS | OK          |
| 12k RPS | OK          |
| 13k RPS | OK          |
| 14k RPS | 💥 DEAD     |

# No warning curve. No gradual degradation.

# Why Horizontal Scaling Fixes This

| Problem           | Horizontal Fix        |
| ----------------- | --------------------- |
| Thread starvation | Add more nodes        |
| Disk IOPS cap     | Shard DB              |
| RAM cache limit   | Distributed cache     |
| Network limit     | Load balancer fan-out |

# So instead of falling off a cliff, the system becomes linearly scalable.





