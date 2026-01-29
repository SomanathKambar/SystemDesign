package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState

interface TokenBucketStore {
    fun get(key: String): TokenBucketState?
    fun save(key: String, state: TokenBucketState)
    
    /**
     * Atomically computes the new state based on the current state.
     * @param key The key to update.
     * @param remappingFunction A function that receives the current state (or null) and returns the new state (or null to remove).
     * @return The new state associated with the key.
     */
    fun compute(key: String, remappingFunction: (TokenBucketState?) -> TokenBucketState?): TokenBucketState?
}
