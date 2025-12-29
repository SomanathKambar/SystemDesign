# Horizontal Scaling (Scale Out)

## Add more machines and distribute traffic.

# Interview-Level Truth
# Horizontal scaling is not “add more nodes”.
# It is a multi-dimensional resource rebalancing problem.

| Layer / Resource         | What Changes / Adds                          | Impact / Result                             | Why It Matters                         |
| ------------------------ | -------------------------------------------- | ------------------------------------------- | -------------------------------------- |
| Load Balancer            | Add one or more LBs                          | Distributes traffic across multiple servers | Prevents single-server overload        |
| API Servers              | Add multiple stateless servers               | RPS scales linearly with nodes              | Handles more concurrent users          |
| Database                 | Sharding & replication                       | Each DB node handles a portion of data      | Avoids single-node DB bottleneck       |
| Cache                    | Distributed cache (Redis Cluster, Memcached) | Hot data available across nodes             | Reduces DB load, improves latency      |
| Network                  | Add multiple NICs, increase bandwidth        | Higher throughput                           | Supports cross-node traffic & load     |
| Health Checks & Failover | Monitoring + auto-restart                    | Nodes can be removed/added safely           | Prevents downtime during failures      |
| DNS / Geo Routing        | Use global DNS + CDN                         | Traffic routed to nearest data center       | Reduces latency for global users       |
| Storage                  | Distributed file systems (S3, GCS, HDFS)     | Data accessible to all nodes                | Supports horizontal scaling of storage |

# Why this matters

Horizontal scaling adds complexity, not just capacity.

Metrics such as RPS per node, cache hit ratio, replication lag, network throughput become critical.

Unlike vertical scaling, failure in one node does not crash the system if horizontal scaling is implemented correctly.

# Horizontal Growth Capacity Table
| Nodes | Safe RPS | Cache Size | DB Shards | Failure Impact   | Notes          |
| ----- | -------- | ---------- | --------- | ---------------- | -------------- |
| 1     | 8k       | 32GB       | 1         | Total outage     | Single node    |
| 2     | 16k      | 64GB       | 1         | 50% traffic lost | DB bottleneck  |
| 4     | 32k      | 128GB      | 2         | 25% traffic lost | Need sharding  |
| 8     | 64k      | 256GB      | 4         | 12% traffic lost | Full HA        |
| 16    | 128k     | 512GB      | 8         | 6% traffic lost  | Regional scale |

# now we can:

### • Predict exactly when DB must be sharded
### • Know how much cache grows
### • Know failure blast radius
### • Know cost growth
### • Prevent the next collapse

# Why horizontal scaling is harder?

| Problem            |
| ------------------ |
| Data consistency   |
| Replication lag    |
| Network partitions |
| Distributed locks  |
| Cache invalidation |
| Leader election    |
| Shard rebalancing  |




# Correct Growth Philosophy
## Vertical first → Horizontal later

| Stage       | Strategy         |
| ----------- | ---------------- |
| Prototype   | Vertical         |
| MVP         | Vertical         |
| Growth      | Vertical         |
| Hypergrowth | Horizontal       |
| Global      | Horizontal + CDN |

# Scaling Summary
| Feature         | Vertical | Horizontal |
| --------------- | -------- | ---------- |
| Complexity      | Low      | High       |
| Cost            | Low      | High       |
| Latency         | Lower    | Higher     |
| Fault tolerance | Low      | High       |
| Maximum scale   | Limited  | Unlimited  |


# Correct scaling decisions save:

## • Money
## • Engineering time
## • Downtime
## • Company survival

# Most startups fail because they go distributed too early.

# Why Horizontal Scaling Fixes Single-node Collapse

| Problem           | Horizontal Fix        |
| ----------------- | --------------------- |
| Thread starvation | Add more nodes        |
| Disk IOPS cap     | Shard DB              |
| RAM cache limit   | Distributed cache     |
| Network limit     | Load balancer fan-out |

# So instead of falling off a cliff, the system becomes linearly scalable.
