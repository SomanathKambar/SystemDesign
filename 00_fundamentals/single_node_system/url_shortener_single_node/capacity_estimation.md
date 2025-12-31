### Daily URLs: 100k
### Avg URL size: 350 bytes
### Storage/day ≈ 20MB
### 1 year ≈ 7.3GB

| Component                         | Per URL         |
| --------------------------------- | --------------- |
| Long URL text                     | 200 B           |
| Short code                        | 8 B             |
| Primary key (BIGINT)              | 8 B             |
| Timestamp                         | 8 B             |
| Row metadata (MVCC, tuple header) | 24–40 B         |
| Indexes (B-Tree)                  | ~60–120 B       |
| Alignment / padding               | ~16 B           |
| **Real DB row size**              | **≈ 320–400 B** |


| Metric                         | Value          |
| ------------------------------ | -------------- |
| URLs / day                     | 100,000        |
| Real row size                  | 350 B          |
| Storage / day                  | ~35 MB         |
| Storage / year                 | ~12.8 GB       |
| 3-year retention               | ~38 GB         |
| Index duplication (replica DB) | 2×             |
| **Total cluster storage**      | **~80–100 GB** |

# Why this matters

Because this determines:

• When our disk fills
• When vacuum slows
• When backups break
• When our DB becomes IO bound
• When we must shard

# How this ties to scaling


| Disk Usage | System State         |
| ---------- | -------------------- |
| <30%       | Fast                 |
| 30–60%     | OK                   |
| 60–80%     | Slower VACUUM        |
| 80–90%     | Query latency spikes |

