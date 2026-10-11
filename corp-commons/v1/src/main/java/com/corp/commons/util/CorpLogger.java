package com.corp.commons.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Corporate logging facade: services log through it so that format and routing
 * can be changed in one place.
 */
public final class CorpLogger {
    private final Logger delegate;

    private CorpLogger(Logger delegate) {
        this.delegate = delegate;
    }

    public static CorpLogger getLogger(Class<?> type) {
        return new CorpLogger(Logger.getLogger(type.getName()));
    }

    public void info(String message, Object... args) {
        delegate.log(Level.INFO, format(message, args));
    }

    public void warn(String message, Object... args) {
        delegate.log(Level.WARNING, format(message, args));
    }

    public void error(String message, Throwable error) {
        delegate.log(Level.SEVERE, message, error);
    }

    static String format(String message, Object[] args) {
        StringBuilder out = new StringBuilder();
        int from = 0;
        for (Object arg : args) {
            int at = message.indexOf("{}", from);
            if (at < 0) {
                break;
            }
            out.append(message, from, at).append(arg);
            from = at + 2;
        }
        return out.append(message.substring(from)).toString();
    }
}
