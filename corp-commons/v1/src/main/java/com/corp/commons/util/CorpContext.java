package com.corp.commons.util;

/**
 * Entry point to the service's corporate environment.
 */
public final class CorpContext {
    private static final CorpContext CURRENT = new CorpContext(new CorpConfig());

    private final CorpConfig config;

    private CorpContext(CorpConfig config) {
        this.config = config;
    }

    public static CorpContext current() {
        return CURRENT;
    }

    public CorpConfig getConfig() {
        return config;
    }

    public String getServiceName() {
        return config.getString("corp.service.name", "unknown-service");
    }
}
