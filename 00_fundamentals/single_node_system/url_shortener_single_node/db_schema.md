## TABLE  urls
### | id | bigint | primary key |
### | short_code | varchar(8) | unique |
### | long_url | text |
### | created_at | timestamp |


# What is this?

This is the database schema for your URL Shortener.

It defines how your URLs are permanently stored.


| Column     | Type       | Meaning                       |
| ---------- | ---------- | ----------------------------- |
| id         | bigint     | Auto-increment unique number  |
| short_code | varchar(8) | The short URL key             |
| long_url   | text       | The original long URL         |
| created_at | timestamp  | When this mapping was created |

## 1. id BIGINT PRIMARY KEY

### A unique number for every row

Used internally by DB

Example:

## id ----- ----- short_code ----- ----- ----- 	long_url
### 1 ---------- -----  -----  aZ93Kq	----- ------ ----- ----- ----- ---- ----- https://google.com

### 2 ----- ----- -----  ----- Xp9LQa	-----  ----- ----- ----- ----- ---   ----- https://youtube.com

### This allows fast indexing and replication.

## 2.short_code VARCHAR(8) UNIQUE

### This is the short URL token.
## Example:
### https://sho.rt/aZ93Kq
                ↑
            short_code
1. Must be unique

2. Fixed length for faster indexing

3. 8 chars Base62 → supports 62⁸ = 218 trillion URLs

## 3. long_url TEXT

### Stores the full long URL: https://www.amazon.in/Some/Very/Long/Path?with=params
### Stored as TEXT because length varies.

## 4 . created_at TIMESTAMP 

### Stores when this URL was created: 2025-01-03 11:32:45
Used for:

Cleanup jobs

Analytics

Expiry

Auditing

## Why this schema is perfect (even at scale)

| Feature                    | Why                     |
| -------------------------- | ----------------------- |
| Unique index on short_code | Fast redirect lookup    |
| Fixed length short_code    | Cache-friendly          |
| Surrogate numeric PK       | DB replication friendly |
| created_at                 | TTL, cleanup, analytics |










