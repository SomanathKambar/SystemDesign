# ENGINEERING PLAYBOOK

## Phase 0 – Requirements
Functional: APIs, flows, user actions  
Non-Functional: Latency, SLA, uptime  
Constraints: Budget, hardware, time  
Scale: Daily users, RPS, storage  
SLA: e.g. 99.99% uptime

Example URL Shortener:
- 100k URLs/day
- 10M redirects/day
- p99 latency < 50ms

---
## Phase 1 – HLD
Components: API, Service, Cache, DB, LB  
Define API contracts, DB schema, failure modes, scaling path

---
## Phase 2 – LLD
Class diagrams, sequence flows, retry logic, caching, sharding

---
## Phase 3 – Implementation Rules
- Clean Architecture
- Infra swappable
- Profiles for environments

---
## Phase 4 – Testing
Smoke, Integration, Load, Chaos, Regression, Stress/Load

---
## Phase 5 – Capacity
Example : 100k URLs/day × 200 bytes ≈ 20MB/day → 7.3GB/year

---
## Phase 6 – Scaling
Vertical: More RAM/CPU  
Horizontal: More nodes, sharding, cache (it's just basic)

---
## Phase 7 – Reliability
Backups, replicas, rate limiters, health checks

---
## Phase 8 – Benchmarks
RPS, p99 latency, CPU, RAM

---
## Phase 9 – Interview
Explain blast radius, tradeoffs, failure modes, scaling paths
