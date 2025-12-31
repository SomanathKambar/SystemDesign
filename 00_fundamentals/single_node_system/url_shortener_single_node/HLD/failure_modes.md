# Failure Modes

# | Failure | Impact | Mitigation |

## |--------|------|-----------|
## DB down | No redirects | Backup DB, periodic snapshots |
## Hash collision | Wrong redirect | Retry with new salt |
## Process crash | Downtime | Systemd auto restart |


| Failure           | What Actually Happens                    | User Impact                   | Root Cause                     | Mitigation                            |
| ----------------- | ---------------------------------------- | ----------------------------- | ------------------------------ | ------------------------------------- |
| DB Down           | Index lookups stall, request queue fills | All short links fail          | Disk IO stall, OOM, corruption | Read replicas, hot standby, PITR      |
| Primary DB Full   | Inserts block, vacuum halts              | New URLs fail, redirects slow | Disk 90%+                      | Disk monitoring + sharding            |
| Hash Collision    | Two URLs share same code                 | Wrong user redirected         | Random collision               | Retry w/ different salt, Bloom filter |
| Process Crash     | App exits                                | 100% outage (single node)     | OOM, panic, segfault           | Supervisor, graceful restart          |
| Memory Leak       | RSS grows                                | Latency explodes              | GC thrash, swap                | Memory profiling, auto restarts       |
| Cache Stampede    | DB flooded                               | Partial outage                | Cache miss storm               | Request coalescing                    |
| Network Partition | App can’t reach DB                       | Redirects hang                | Switch failure                 | Timeouts + retries                    |
| Clock Skew        | Expiry jobs break                        | URLs expire early/late        | Bad NTP                        | NTP sync                              |
| Bad Deploy        | New code crashes                         | All traffic down              | Rollout bug                    | Canary deploy                         |
| DDOS              | Traffic overload                         | 502 errors                    | Bot flood                      | Rate limiter + WAF                    |


# Why this matters

Now we can:

• Predict blast radius
• Design redundancy
• Build runbooks
• Prevent outages

# 1.Blast radius = How much of our system dies when one thing breaks
| Failure                 | Blast Radius          |
| ----------------------- | --------------------- |
| Single Node Crash       | 100% outage           |
| One App Node Crash (HA) | 50% slow, still works |
| One DB Shard Crash      | 12% traffic impacted  |
| One Region Down         | 50% global traffic    |

### Being able to predict blast radius means:

### We know beforehand what % of users will be affected.

### we can design to contain damage.

# 2.Design Redundancy
# Redundancy = Intentionally duplicating critical components

| Component  | Without Redundancy | With Redundancy             |
| ---------- | ------------------ | --------------------------- |
| App Server | Full outage        | Load balancer routes around |
| Database   | Total outage       | Hot standby / replica       |
| Cache      | DB flood           | Distributed cache           |
| Network    | Isolation          | Multi-AZ routing            |

## Redundancy is what converts: Single point of failure → Highly Available system

# 3.Build Runbooks

# Runbooks = Step-by-step emergency playbooks

| Incident    | Runbook Action            |
| ----------- | ------------------------- |
| DB disk 90% | Add shard, reroute writes |
| Replica lag | Promote standby           |
| Memory leak | Rolling restart           |
| DDOS        | Enable WAF + rate limit   |


# Runbooks let engineers fix outages in minutes instead of hours.


# 4.Prevent Outages

Because we know:

• exactly when disk fills
• when CPU hits collapse
• when to shard
• when to add nodes
• how failure propagates

we don’t wait for outages — we design them out.
