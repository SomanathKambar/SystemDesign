# Deep Dive: Hashing for Deduplication

In our "Make it Correct" phase, we introduced **SHA-256 Hashing** to handle deduplication. Here is the engineering reasoning behind this decision.

## 1. The Problem: "The Fat Index"

If we want to ensure we don't store duplicate URLs, we need to ask the database:
> *"Do you already have `https://very-long-domain.com/some/extremely/long/path/query?params=...`?"*

### Option A: Index the `long_url` column directly
*   **Pros:** Simple.
*   **Cons:**
    *   **Space:** URLs can be up to 2,048 characters. A B-Tree index on a `VARCHAR(2048)` column is massive.
    *   **Performance:** Comparing long strings is CPU intensive for the DB.
    *   **Memory Pressure:** Fewer index entries fit in RAM, leading to more disk I/O (swapping).

### Option B: The "Fingerprint" Strategy (Our Choice)
Instead of indexing the URL, we compute a mathematical fingerprint (Hash) and index *that*.

```kotlin
val hash = SHA256("https://google.com") 
// Result: "05046f88..." (64 characters)
```

*   **Pros:**
    *   **Fixed Size:** Every hash is exactly 64 characters. The database loves predictable sizes.
    *   **Fast:** The DB only compares 64-char strings, never 2000-char strings.
    *   **Uniformity:** Hashes are random-looking, meaning the B-Tree stays balanced.

---

## 2. Why SHA-256? (Trade-offs)

We chose **SHA-256** (Secure Hash Algorithm 256-bit).

| Algorithm | Output Size | Security/Collision Risk | Speed | Verdict |
| :--- | :--- | :--- | :--- | :--- |
| **MD5** | 32 chars | **High Risk.** Broken. Easy to generate collisions. | Very Fast | ❌ Unsafe |
| **SHA-1** | 40 chars | **Medium Risk.** Technically broken by Google/CWI. | Fast | ❌ Avoid |
| **SHA-256** | 64 chars | **Near Zero Risk.** Standard for security. | Moderate | ✅ **Winner** |
| **Murmur3** | 32/64 bits | **Non-Cryptographic.** Fast, but higher collision chance. | Blazing | ❌ Good for HashMaps, not DB keys |

### "What if two URLs have the same Hash?"
This is called a **Hash Collision**.
*   With SHA-256, the probability is roughly $1$ in $10^{77}$.
*   To put that in perspective: You are more likely to be struck by lightning *while* winning the lottery *while* being bitten by a shark... **every day for a year**.
*   For our system, we treat it as "Impossible."

---

## 3. The Flow

1.  **User sends:** `https://google.com`
2.  **Service computes:** `hash = SHA256("https://google.com")`
3.  **Service asks DB:** `SELECT * FROM urls WHERE url_hash = ?`
    *   **FOUND:** Return existing `short_code` (e.g., `ABC12345`).
    *   **NOT FOUND:** 
        1.  Generate new code.
        2.  Insert `(short_code, long_url, url_hash)`.

## 4. Normalization (The Hidden Trap)

Before hashing, we must **Normalize**.

*   `https://google.com`
*   `https://google.com/`
*   `HTTPS://GOOGLE.COM`

To a computer, these are 3 different strings. They would produce **3 different hashes**.
Our code does:
1.  **Trim** whitespace.
2.  **Remove trailing slash** (so `google.com/` == `google.com`).
3.  (Ideally) **Lowercase domain**, but keep path case-sensitive (since `bit.ly/ABC` != `bit.ly/abc`).

---

## 5. Senior Engineer Note: "The Birthday Paradox"

While SHA-256 collisions are impossible in practice, our **Short Codes** (8 characters) *will* have collisions.
*   We use 62 chars `[A-Za-z0-9]`.
*   $62^8 \approx 218$ trillion combinations.
*   This is huge, but not infinite.
*   That is why we need the `retry` loop in `UrlService.kt`. We *expect* short code collisions eventually, but we *never* expect Hash collisions.
