package com.miftah.core_bank_system.security;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

@Service
public class RateLimitingService {

    @Value("${application.security.rate-limit.login:10}")
    private int loginLimit;

    @Value("${application.security.rate-limit.register:3}")
    private int registerLimit;

    @Value("${application.security.rate-limit.refresh:10}")
    private int refreshLimit;

    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

    public Bucket getLoginBucket(String ip) {
        return loginBuckets.computeIfAbsent(ip, k -> createNewBucket(loginLimit, 1));
    }

    public Bucket getRegisterBucket(String ip) {
        return registerBuckets.computeIfAbsent(ip, k -> createNewBucket(registerLimit, 1));
    }

    public Bucket getRefreshBucket(String ip) {
        return refreshBuckets.computeIfAbsent(ip, k -> createNewBucket(refreshLimit, 1));
    }

    private Bucket createNewBucket(int limit, int minutes) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(minutes))))
                .build();
    }
}
