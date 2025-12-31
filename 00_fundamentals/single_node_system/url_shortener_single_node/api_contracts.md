## Defines how clients talk to  system.

POST /shorten
{
"longUrl": "https://example.com/abc"
}

## Meaning:

### Client is asking server:

### “Store this URL and give me a short token.”

### GET /{shortCode}
### → Redirect 302 to long URL (This is why  DB schema must support fast lookup by short_code.)

# What is “fast lookup”?

## When someone opens: https://sho.rt/aZ93Kq

###  server must do one critical operation: “Find the long URL that matches aZ93Kq.”

## That is a lookup operation:
## SELECT long_url FROM urls WHERE short_code = 'aZ93Kq';
### This must happen millions of times per second in real systems.

# Why must it be fast?

## Redirects are:

• User-facing
• Blocking (browser waits)
• Extremely high traffic

### If this lookup is slow:
| Result             |
| ------------------ |
| Users see delay    |
| Browsers retry     |
| Traffic multiplies |
| Servers collapse   |
| Revenue is lost    |

## So  entire system success depends on lookup speed.

# Why lookup can fail

| Failure       | What happens          |
| ------------- | --------------------- |
| No index      | DB scans entire table |
| Large table   | Disk I/O explosion    |
| Hot keys      | Lock contention       |
| Cold cache    | Disk seeks            |
| Network delay | Timeout               |


# Why we made short_code UNIQUE + FIXED LENGTH
| Design           | Why                           |
| ---------------- | ----------------------------- |
| UNIQUE index     | B-Tree index enables O(log N) |
| Fixed length (8) | Cache line efficient          |
| Small key size   | Fits in memory                |
| High cardinality | Uniform shard distribution    |

## This makes: short_code → long_url
## a near-memory-speed operation.


# Why HTTP 302 specifically?

## HTTP 302 means:

## “The resource moved temporarily — redirect client to new URL.”

| Code | Browser action             |
| ---- | -------------------------- |
| 301  | Permanent cache            |
| 302  | Temporary redirect         |
| 307  | Strict method preservation |

##  URL shorteners use 302 because:

• We may change mapping
• We want to track analytics
• We don’t want browsers to cache forever

##  So every click hits  server → We retain control.


## Full redirect flow

### Browser → GET /aZ93Kq
### Server → DB lookup (fast)
### Server → 302 Location: https://amazon.com/...
### Browser → Loads Amazon

 DB lookup is the single most critical operation in  system.

### (This one design decision determines:)
### • Cost
### • Speed
### • Scalability
### • Reliability
### • Business success







