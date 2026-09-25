package com.influencerapp.test.utils;

import java.util.UUID;

/**
 * Utility for generating non-colliding test data across isolated test executions.
 */
public final class TestDataGenerator {

    private TestDataGenerator() {
    }

    public static String generateUniqueEmail() {
        return "testuser_" + UUID.randomUUID().toString().substring(0, 8) + "@influencertest.io";
    }

    public static String generateValidPassword() {
        return "StrongPass123!" + UUID.randomUUID().toString().substring(0, 4);
    }

    public static String generateChannelName() {
        return "Channel_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static String generateIdempotencyKey() {
        return UUID.randomUUID().toString();
    }
}
