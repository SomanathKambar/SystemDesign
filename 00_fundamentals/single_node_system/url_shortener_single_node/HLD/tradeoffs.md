# Tradeoffs

| Decision    | Benefit           | Cost           |
| ----------- | ----------------- | -------------- |
| Single DB   | Simple            | Scaling limits |
| Random hash | Even distribution | No ordering    |

| Decision          | Benefit         | What You Gain (Numbers) | What You Lose               | When It Breaks               | Why It Matters          |
| ----------------- | --------------- | ----------------------- | --------------------------- | ---------------------------- | ----------------------- |
| Single DB         | Simple          | <5 ms reads, 8k RPS     | Hard 8k–12k RPS ceiling     | DB saturation → total outage | Single point of failure |
| Random hash       | Even shard load | Balanced shards ±5%     | No locality, hard analytics | Cross-shard queries          | Complicates analytics   |
| Base62 short code | Compact URLs    | 218T combinations       | Collision risk              | High volume                  | Needs retry logic       |
| In-memory cache   | Fast redirects  | <1 ms lookup            | Memory limited              | Eviction storms              | Cache stampede risk     |
| No auth           | Easy usage      | Zero friction           | Abuse & DDOS                | Bot floods                   | Needs rate limiting     |
| 302 redirect      | SEO friendly    | Preserves link juice    | Extra RTT                   | Latency                      | Slower redirects        |
| TTL expiry        | Storage control | Auto cleanup            | Broken old links            | Data loss                    | Must communicate TTL    |


# Why this matters
• Explains consequences
• Shows scaling thresholds
• Predicts failure conditions
• Helps architectural decisions
• Makes interviews trivial
