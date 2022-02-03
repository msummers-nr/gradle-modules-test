package com.newrelic.opentracing.logging;

import java.util.List;

public interface Logger {

    /**
     * Writes to standard out. Should only be used to write payload data.
     */
    void out(String message);

    /**
     * Writes to standard out. Can be used to also write debug messages for trouble-shooting.
     */
    void debug(String message);

    /**
     * Return a list of all logged messages. In most implementations this will be a no-op.
     */
    List<String> getLogs();

}
