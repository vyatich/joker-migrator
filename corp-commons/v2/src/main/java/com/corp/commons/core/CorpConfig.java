package com.corp.commons.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Service settings: {@code corp.properties} from the classpath, overridden by
 * system properties with the same keys.
 */
public final class CorpConfig {
    private final Properties properties = new Properties();

    CorpConfig() {
        try (InputStream in = CorpConfig.class.getClassLoader().getResourceAsStream("corp.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getString(String key, String defaultValue) {
        return System.getProperty(key, properties.getProperty(key, defaultValue));
    }

    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        return value == null ? defaultValue : Integer.parseInt(value.trim());
    }

    public long getLong(String key, long defaultValue) {
        String value = getString(key, null);
        return value == null ? defaultValue : Long.parseLong(value.trim());
    }
}
