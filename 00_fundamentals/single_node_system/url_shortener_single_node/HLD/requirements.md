## Functional
- Convert long URL → short URL
- Redirect short URL → original URL


| Requirement  | Why                    |
| ------------ | ---------------------- |
| Long → Short | Business feature       |
| Redirect     | Core purpose of system |



## Non-Functional
- < 50ms latency
- High read throughput
- Durable storage

| Metric     | Why                |
| ---------- | ------------------ |
| Latency    | User experience    |
| Throughput | Can handle traffic |
| Durability | Never lose URLs    |

These are engineering constraints, not features.

## Out of Scope
- Custom aliases
- Analytics

