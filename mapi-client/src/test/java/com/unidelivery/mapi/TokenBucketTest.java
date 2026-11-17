package com.unidelivery.mapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketTest {

    @Test
    void burstAllowedThenSmoothed() {
        TokenBucket bucket = new TokenBucket(5, 5);
        long start = System.nanoTime();
        for (int i = 0; i < 5; i++) {
            bucket.acquire();
        }
        long burstMs = (System.nanoTime() - start) / 1_000_000;
        assertTrue(burstMs < 100, "burst 应瞬时: " + burstMs);
        long sixth = System.nanoTime();
        bucket.acquire();
        long waitMs = (System.nanoTime() - sixth) / 1_000_000;
        assertTrue(waitMs >= 150, "第 6 个令牌应等待约 200ms: " + waitMs);
    }

    @Test
    void highRateDoesNotBlock() {
        TokenBucket bucket = new TokenBucket(1000, 1000);
        long start = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            bucket.acquire();
        }
        assertTrue((System.nanoTime() - start) / 1_000_000 < 100);
    }
}
