# Low Level Design: Sliding Window Strategy

## 1. Algorithm: Sliding Window Log
Unlike Fixed Window which buckets time into static segments, Sliding Window Log tracks every request's timestamp. When a new request arrives, it removes all logs older than `now - windowSize` and checks if the remaining count is within the limit.

## 2. Logic Flow
1. **Request Received**: `allow(key)` called.
2. **Cleanup**: Remove timestamps `T` where `T < (now - windowSizeMs)`.
3. **Check**: Count remaining timestamps in the log.
4. **Decision**:
    - If `count < limit`:
        - Add `now` to log.
        - Return `Decision(allowed=true)`.
    - Else:
        - `oldestInWindow = log.first()`
        - `retryAfter = windowSizeMs - (now - oldestInWindow)`
        - Return `Decision(allowed=false, retryAfter)`.

## 3. Data Structure
For in-memory, a `java.util.Deque` or a `SortedSet` can be used. For distributed, a Redis **Sorted Set (ZSET)** is ideal, where the score and value are both the timestamp.

## 4. Class Design

### `SlidingWindowRateLimiter`
- **Properties**: `limit`, `windowSizeMs`.
- **State Store Requirement**: Must support `getLog(key)`, `addLog(key, timestamp)`, and `cleanup(key, beforeTimestamp)`.

### `SlidingWindowStore` (Extended Interface)
We will extend the base `StateStore` or add specific methods to handle lists of timestamps.

## 5. Complexity
- **Time**: `O(D)` where `D` is the number of expired entries to delete. Typically very fast if cleanup is done per request.
- **Space**: `O(N)` where `N` is the number of requests in the current window.
