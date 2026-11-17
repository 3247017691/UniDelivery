package com.unidelivery.mapi;

/**
 * 令牌桶限流器（单实例部署假设；水平扩展需外置限流，见 runbook）。
 * 平滑限速 + 允许 burst 个瞬时令牌。acquire 阻塞到拿到令牌为止。
 */
public final class TokenBucket {

    private final double ratePerSecond;
    private final double capacity;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(double ratePerSecond, double capacity) {
        if (ratePerSecond <= 0 || capacity <= 0) {
            throw new IllegalArgumentException("rate/capacity 必须为正");
        }
        this.ratePerSecond = ratePerSecond;
        this.capacity = capacity;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized void acquire() {
        while (true) {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return;
            }
            double deficit = 1.0 - tokens;
            long waitMillis = (long) Math.ceil(deficit / ratePerSecond * 1000.0);
            try {
                wait(Math.max(1L, waitMillis));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("限流等待被中断", e);
            }
        }
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        if (elapsedSeconds > 0) {
            tokens = Math.min(capacity, tokens + elapsedSeconds * ratePerSecond);
            lastRefillNanos = now;
        }
    }
}
