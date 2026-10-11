package com.corp.commons.core;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Corporate logging facade: services log through it so that format and routing
 * can be changed in one place.
 */
public final class CorpLog {
    private final Logger delegate;

    private CorpLog(Logger delegate) {
        this.delegate = delegate;
    }

    public static CorpLog getLogger(Class<?> type) {
        return new CorpLog(Logger.getLogger(type.getName()));
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
        var out = new StringBuilder();
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
