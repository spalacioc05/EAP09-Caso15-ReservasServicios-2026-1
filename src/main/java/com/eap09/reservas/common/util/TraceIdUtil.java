package com.eap09.reservas.common.util;

import org.slf4j.MDC;

public final class TraceIdUtil {

    private TraceIdUtil() {
    }

    public static String currentTraceId() {
        return MDC.get("traceId");
    }
}
