package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.TokenBucketState

interface TokenBucketStore : StateStore<TokenBucketState> {
    // Legacy support or specific methods can go here
    // For now, it just inherits from generic StateStore
}