# Low Level Design: Token Bucket Strategy

## 1. Algorithm Overview
The **Token Bucket** algorithm maintains a "bucket" that holds tokens. 
- Tokens are added to the bucket at a fixed rate (`refillRate`).
- The bucket has a maximum capacity (`capacity`).
- Each request consumes one token.
- If the bucket is empty, the request is denied.

This algorithm is superior for many use cases because it allows for **bursts** (up to the bucket capacity) but enforces a strict long-term average rate.

## 2. Logic Flow
1. **Request Received**: `allow(key)` called.
2. **Refill Logic (Lazy)**: 
    - Calculate `timePassed = now - lastRefillTime`.
    - `tokensToAdd = timePassed * (refillRate / 1000)`.
    - `newTokens = min(capacity, currentTokens + tokensToAdd)`.
3. **Decision**:
    - If `newTokens >= 1`:
        - `currentTokens = newTokens - 1`.
        - `lastRefillTime = now`.
        - Return `Decision(allowed=true)`.
    - Else:
        - `timeToNextToken = (1 / refillRate) - (now - lastRefillTime)`.
        - Return `Decision(allowed=false, retryAfterMs)`.

## 3. Data Structure
### `TokenBucketState`
```kotlin
data class TokenBucketState(
    val tokens: Double,
    val lastRefillTime: Long
)
```

## 4. Class Design
### `TokenBucketRateLimiter`
- **Properties**:
    - `capacity`: Maximum tokens in the bucket.
    - `refillTokensPerSecond`: Rate at which tokens are added.
- **Dependencies**: `TokenBucketStore`, `Clock`.

## 5. Complexity
- **Time**: `O(1)` - Simple arithmetic.
- **Space**: `O(1)` per key - Stores 2 numbers (tokens and timestamp).
