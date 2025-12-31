# 1. System Goal

## Build a minimal, correct, and high-performance URL shortener that:

• Creates short URLs
• Redirects users reliably
• Works on a single machine
• Can later evolve into scaled and distributed versions

## Client → REST API → Hash Generator → DB → Redirect Service

# Flow:

## 1.Client sends long URL

## 2.Server generates Base62 hash

## 3.Stores in DB

## 4.Returns short URL

## 5.Redirect uses DB lookup


# 2. Functional Requirements

| Requirement       | Description                                     |
| ----------------- | ----------------------------------------------- |
| Create Short URL  | Generate a unique short code for a long URL     |
| Redirect          | Redirect to original long URL using short code  |
| Uniqueness        | Same long URL may generate different short URLs |
| Availability      | System must serve redirects with low latency    |
| Persistence       | All mappings must survive restarts              |
| Expiry (Optional) | URLs may expire in future                       |


# 3. Non-Functional Requirements

| Metric           | Target           |
| ---------------- | ---------------- |
| Redirect Latency | < 50 ms          |
| Throughput       | 8k redirects/sec |
| Durability       | No data loss     |
| Consistency      | Strong           |
| Availability     | Best effort      |
| Scalability      | Single node only |

# 4. High Level Architecture
                        Client
                           |
                           v
                        HTTP Server
                           |
                           v
                        Redirect Service
                           |
                           v
                        Local Cache (OS page cache)
                           |
                           v
                        Primary Database (Postgres / MySQL)


# 5. Component Responsibilities

| Component            | Responsibility        |
| -------------------- | --------------------- |
| HTTP Server          | Accept requests       |
| Redirect Service     | Business logic        |
| Cache                | Speed up hot lookups  |
| Primary DB           | Persistent storage    |
| Short Code Generator | Generate unique codes |

# 6. Data Flow

# Create URL
## Client → POST /shorten → Service → Generate Code → Insert DB → Return short URL

# Redirect
## Client → GET /{code} → Cache lookup → DB lookup → 302 redirect


# 7. Storage Model
| Entity      | Stored In |
| ----------- | --------- |
| URL Mapping | DB        |
| Cache       | Memory    |
| Logs        | Disk      |

# 8. Capacity Planning (Single Node)
| Metric         | Value  |
| -------------- | ------ |
| Safe RPS       | 8,000  |
| Max RPS        | 12,000 |
| Daily URLs     | 100k   |
| 1 Year Storage | ~13 GB |

# 9. Failure Model

| Failure         | Behavior         |
| --------------- | ---------------- |
| App crash       | Full outage      |
| DB crash        | Full outage      |
| Disk full       | New URLs blocked |
| Memory pressure | Latency spike    |

# 10. Limitations

• Single point of failure
• Hard throughput ceiling
• No redundancy
• No auto-recovery
• No multi-region support

# 11. Why This Design Exists

This version is the baseline correctness model — everything you build later must preserve its behavior.





