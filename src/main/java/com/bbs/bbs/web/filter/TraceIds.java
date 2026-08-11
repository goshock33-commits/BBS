package com.bbs.bbs.web.filter;

import org.slf4j.MDC;

public final class TraceIds {
    public static final String MDC_KEY = "traceId";

    private TraceIds() {
    }

    public static String current() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null ? "unknown" : traceId;
    }
}
