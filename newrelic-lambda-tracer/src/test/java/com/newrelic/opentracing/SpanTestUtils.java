package com.newrelic.opentracing;

import java.util.Map;

public class SpanTestUtils {

    public static LambdaSpan createSpan(String operationName, long timestamp, long nanoTime, Map<String, Object> tags, LambdaSpan parentSpan, String guid,
            String txnId) {
        final LambdaSpan span = new LambdaSpan(operationName, timestamp, nanoTime, tags, parentSpan, guid, txnId);
        final LambdaSpanContext context = new LambdaSpanContext(span, new LambdaScopeManager());
        span.setContext(context);
        return span;
    }

}
