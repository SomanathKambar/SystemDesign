package com.systemdesign.infra.ratelimiter.core

import com.systemdesign.infra.ratelimiter.core.model.LeakyBucketState

interface LeakyBucketStore : StateStore<LeakyBucketState>
