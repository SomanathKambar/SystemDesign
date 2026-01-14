# Scaling Models

| Scaling Type | What It Means  | How It Works      | What It Solves  | What Breaks     | Where It Fails  | When To Use    | Real Numbers |
| ------------ | -------------- | ----------------- | --------------- | --------------- | --------------- | -------------- | ------------ |
| Vertical     | Bigger machine | More CPU/RAM/IOPS | Simplicity      | Hardware limits | ~128 GB RAM cap | MVP, POC       | ~10k rps     |
| Horizontal   | More nodes     | Load balancer     | Unlimited scale | Consistency     | Hot keys        | Internet apps  | 100k–10M rps |
| Hybrid       | Both           | Combine           | Cost + scale    | Complexity      | Ops mistakes    | SaaS platforms | 10M+ rps     |

# Caching
| Cache Type | Where    | Why          | What It Accelerates | TTL     | What Can Break | Failure Impact |
| ---------- | -------- | ------------ | ------------------- | ------- | -------------- | -------------- |
| In-memory  | Same JVM | 1–5 ms reads | Hot URLs            | Seconds | Restart loses  | Slow only      |
| Redis      | Network  | 0.5 ms       | Multi-node          | Minutes | Redis down     | Partial outage |
| CDN        | Edge     | 0 ms         | Global users        | Hours   | Stale cache    | Wrong content  |

# Sharding
| Shard Type   | How            | Why       | Problem Solved | Breaks      | Recovery     |
| ------------ | -------------- | --------- | -------------- | ----------- | ------------ |
| Hash shard   | hash(key)%N    | Even load | Hot DB         | Rebalancing | Hash rings   |
| Prefix shard | By first chars | Locality  | Geo scale      | Hot prefix  | Add salt     |
| Range shard  | ID ranges      | Ordering  | Analytics      | Hot ranges  | Split shards |

# Capacity Estimation
| Metric       | Meaning       | Example | Impact       |
| ------------ | ------------- | ------- | ------------ |
| RPS          | Requests/sec  | 5k      | CPU sizing   |
| Avg payload  | Bytes         | 200 B   | Bandwidth    |
| Daily volume | Total req/day | 400M    | Storage      |
| TTL          | Data lifetime | 365d    | Cleanup jobs |

# Failure Modes

| Failure    | What Happens     | Blast Radius | Detection     | Mitigation     |
| ---------- | ---------------- | ------------ | ------------- | -------------- |
| DB down    | No redirects     | 100% outage  | Health check  | Replica        |
| Redis down | Slow reads       | Partial      | Latency alert | Fallback cache |
| LB failure | No traffic       | Total        | Heartbeat     | Multi-LB       |
| Node crash | 1/N traffic lost | Small        | K8s           | Auto restart   |


# Trade-Off Matrix

| Decision    | Why Chosen  | Benefit     | Cost     | Hidden Risk    |
| ----------- | ----------- | ----------- | -------- | -------------- |
| Single DB   | MVP speed   | Simple      | No scale | Rewrites later |
| Redis       | Speed       | Low latency | Ops cost | Single point   |
| Random hash | Even spread | Easy        | No order | Debug pain     |

# Cost of Redirect

| Step  | What Happens | Avg Latency | Risk         |
| ----- | ------------ | ----------- | ------------ |
| LB    | Route        | 1 ms        | Misroute     |
| JVM   | Parse        | 1 ms        | GC pause     |
| Cache | Lookup       | 0.5 ms      | Cache miss   |
| DB    | Index scan   | 2 ms        | IO wait      |
| 302   | Send         | 1 ms        | Client delay |

# Horizontal Growth Capacity

| Nodes | RPS | Cost | Failure Impact |
| ----- | --- | ---- | -------------- |
| 1     | 5k  | $    | Total          |
| 3     | 15k | $$   | Partial        |
| 10    | 50k | $$$  | Low            |


