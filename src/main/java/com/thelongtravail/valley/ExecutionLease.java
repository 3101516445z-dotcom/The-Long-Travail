package com.thelongtravail.valley;

public final class ExecutionLease {
    public final long limit;
    private long expires;

    public ExecutionLease(long expires, long limit) {
        this.limit = limit;
        this.expires = Math.min(expires, limit);
    }

    public static ExecutionLease start(long now) {
        long limit = now + 20L * com.thelongtravail.config.AzraelConfig.WATCH_SECONDS.get();
        return new ExecutionLease(now + 200, limit);
    }

    public long expires() {
        return expires;
    }

    public boolean expired(long now) {
        return now >= expires;
    }

    public void restored() {
        expires = limit;
    }

    public void update(long expiry) {
        expires = Math.min(limit, Math.max(expires, expiry));
    }
}
