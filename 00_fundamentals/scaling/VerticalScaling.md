# Scaling Fundamentals — Vertical vs Horizontal Growth

### This document explains how systems grow from a single cheap server into planet-scale infrastructure.

## 1. Vertical Scaling (Scale Up)

### Add more power to the same machine.


# What changes?
## Vertical Scaling — What Actually Increases
### Vertical scaling = same machine, more power.

| Resource                  | What Increases                               | Why It Matters             |
| ------------------------- | -------------------------------------------- | -------------------------- |
| **CPU cores**             | Threads, parallel requests, encryption speed | More simultaneous users    |
| **Clock speed**           | Single-request latency                       | Faster response time       |
| **RAM**                   | In-memory cache size, DB index cache         | More hot data stays in RAM |
| **Disk (NVMe)**           | IOPS, sequential read speed                  | Faster DB access           |
| **Network bandwidth**     | Mbps/Gbps                                    | More redirects/sec         |
| **File descriptor limit** | Open connections                             | Prevent socket exhaustion  |
| **Kernel buffers**        | Packet throughput                            | Less packet loss           |
| **DB buffer pool**        | Page cache hit ratio                         | Less disk IO               |
| **OS page cache**         | Read latency                                 | Faster DB + file access    |


# Why each one directly increases capacity

| Bottleneck           | What removes it      |
| -------------------- | -------------------- |
| CPU saturated        | More cores           |
| Slow DB              | Bigger RAM + NVMe    |
| Too many connections | Higher FD limits     |
| Network maxed        | Higher NIC bandwidth |


# Real Example
Upgrade:

| Before            | After               |
| ----------------- | ------------------- |
| 4 cores / 8GB RAM | 16 cores / 32GB RAM |



# Results:

## • RPS jumps from ~2k → ~8k
## • Latency drops 3×
## • Disk IO drops 90%
## • Cache hit rate goes from 40% → 95%

# This helps to understand why vertical scaling is not “just add RAM” —
# it is removing bottlenecks layer by layer.

## Why vertical scaling is always first

| Reason                    |
| ------------------------- |
| Cheapest                  |
| Simplest                  |
| No distributed complexity |
| No data sharding          |
| No replication bugs       |
| No network partitions     |

### Most startups should live here as long as possible.

# Vertical Growth Path (Real Numbers)

| Stage            | Machine        | Redirect Capacity |
| ---------------- | -------------- | ----------------- |
| MVP              | 2 vCPU / 4GB   | ~1k RPS           |
| Early Growth     | 4 vCPU / 8GB   | ~2k RPS           |
| Growth           | 8 vCPU / 16GB  | ~4k RPS           |
| Peak Single Node | 16 vCPU / 32GB | ~8k RPS           |
| Max Vertical     | 32 vCPU / 64GB | ~15k RPS          |


At peak, this is ~1.3 BILLION redirects/day on one machine.

# Step-by-Step Growth Path (Single Node)

| Stage    | Machine          | What you do         | Why                |
| -------- | ---------------- | ------------------- | ------------------ |
| MVP      | 2 vCPU / 4GB RAM | Basic DB, no cache  | Cheapest           |
| Growth   | 4 vCPU / 8GB     | Add indexes         | Handle traffic     |
| Scale-Up | 8 vCPU / 16GB    | Add in-memory cache | Reduce DB load     |
| Peak     | 16 vCPU / 32GB   | Tune DB, SSD, IO    | Max capacity       |
| Limit    | 32+ vCPU / 64GB  | OS & DB tuning      | Last vertical step |


# When vertical scaling fails

| Bottleneck               | Observed Result                      | Why It Happens                                 |
| ------------------------ | ------------------------------------ | ---------------------------------------------- |
| CPU Saturated            | Requests queue up, high latency      | All cores busy, cannot process more threads    |
| RAM Exhausted            | DB or cache thrashing, slow response | Hot data cannot fit in memory → disk IO spikes |
| Disk IOPS Maxed          | DB read/write slow, latency spikes   | SSD cannot serve more reads/writes             |
| Network Saturated        | Dropped connections, slow redirects  | NIC bandwidth limit reached                    |
| Latency SLA Broken       | Users experience > 50ms redirect     | Combined resource limits exceed SLA            |
| OS/File Descriptor Limit | Connection errors, failed requests   | Too many concurrent sockets open               |
| Cache Miss Rate High     | Increased DB load, slower response   | RAM too small to hold hot dataset              |


# Why this matters

Provides objective thresholds for vertical growth.

Helps decide exactly when horizontal scaling is needed.

Turns vague “machine is overloaded” into quantifiable triggers.

# What changes as memory grows?
| RAM  | What improves           |
| ---- | ----------------------- |
| 4GB  | Minimal OS cache        |
| 8GB  | DB index fits           |
| 16GB | Hot URLs cached         |
| 32GB | Almost all reads in RAM |
| 64GB | Near in-memory DB       |

# Latency drops from 20ms → <1ms.

# Why this is gold

## Because:

### • You save money
### • You avoid premature distributed complexity
### • You can handle millions of users on one box
### • You understand real bottlenecks

# This is how 90% of startups scale.

# When single node is not enough?

## When:

### • CPU saturated
### • Network saturated
### • Disk IOPS saturated
### • Latency SLA breaks

Only then distribute.

## How many redirects can Single-Node handle?




