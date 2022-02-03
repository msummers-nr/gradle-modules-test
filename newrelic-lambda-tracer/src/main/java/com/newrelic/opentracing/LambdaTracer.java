package com.newrelic.opentracing;

import com.newrelic.opentracing.dt.DistributedTracePayload;
import com.newrelic.opentracing.dt.DistributedTracePayloadImpl;
import com.newrelic.opentracing.logging.Log;
import com.newrelic.opentracing.util.Base64;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

public class LambdaTracer implements Tracer {

    private static final String NEWRELIC_TRACE_HEADER = "newrelic";
    private static final Charset UTF_8_CHARSET = Charset.forName("UTF-8");

    public static final LambdaTracer INSTANCE = new LambdaTracer();

    private final LambdaScopeManager scopeManager = new LambdaScopeManager();
    private final AdaptiveSampling adaptiveSampling = new AdaptiveSampling();

    private LambdaTracer() {
    }

    @Override
    public ScopeManager scopeManager() {
        return scopeManager;
    }

    @Override
    public Span activeSpan() {
        Scope scope = scopeManager.active();
        return scope == null ? null : scope.span();
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
        return new LambdaSpanBuilder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        if (!(spanContext instanceof LambdaSpanContext)) {
            return;
        }

        LambdaSpanContext lambdaSpanContext = (LambdaSpanContext) spanContext;
        final LambdaSpan span = lambdaSpanContext.getSpan();
        DistributedTracePayload distributedTracePayload = lambdaSpanContext.getDistributedTracingState().createDistributedTracingPayload(span);

        if (distributedTracePayload == null) {
            return;
        }

        if (format.equals(Format.Builtin.TEXT_MAP)) {
            ((TextMap) carrier).put(NEWRELIC_TRACE_HEADER, distributedTracePayload.text());
        } else if (format.equals(Format.Builtin.HTTP_HEADERS)) {
            ((TextMap) carrier).put(NEWRELIC_TRACE_HEADER, distributedTracePayload.httpSafe());
        } else if (format.equals(Format.Builtin.BINARY)) {
            // First, specify length of distributed trace payload as an index.
            byte[] payloadBytes = distributedTracePayload.text().getBytes(UTF_8_CHARSET);
            ((ByteBuffer) carrier).putInt(payloadBytes.length);
            ((ByteBuffer) carrier).put(payloadBytes);
        }
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        String payload = getPayloadString(format, carrier);
        if (payload == null) {
            return null;
        }

        DistributedTracePayloadImpl distributedTracePayload = DistributedTracePayloadImpl.parseDistributedTracePayload(payload);
        if (distributedTracePayload == null) {
            String msg = MessageFormat.format("{0} header value was not accepted.", NEWRELIC_TRACE_HEADER);
            Log.getInstance().debug(msg);
            throw new IllegalArgumentException(msg);
        }

        long transportDurationInMillis = Math.max(0, System.currentTimeMillis() - distributedTracePayload.timestamp);
        return new LambdaPayloadContext(distributedTracePayload, transportDurationInMillis, Collections.emptyMap());
    }

    private <C> String getPayloadString(Format<C> format, C carrier) {
        String payload = null;
        if (format.equals(Format.Builtin.TEXT_MAP)) {
            for (Map.Entry<String, String> entry : ((TextMap) carrier)) {
                if (entry.getKey().equalsIgnoreCase(NEWRELIC_TRACE_HEADER)) {
                    payload = entry.getValue();
                }
            }
        } else if (format.equals(Format.Builtin.HTTP_HEADERS)) {
            if (((TextMap) carrier).iterator() == null) {
                throw new IllegalArgumentException("Invalid carrier.");
            }

            for (Map.Entry<String, String> entry : ((TextMap) carrier)) {
                if (entry.getKey().equalsIgnoreCase(NEWRELIC_TRACE_HEADER)) {
                    payload = new String(Base64.decode(entry.getValue()), UTF_8_CHARSET);
                }
            }
        } else if (format.equals(Format.Builtin.BINARY)) {
            ByteBuffer buffer = (ByteBuffer) carrier;
            if (buffer == null) {
                throw new IllegalArgumentException("Invalid carrier.");
            }

            int payloadLength = buffer.getInt();
            byte[] payloadBytes = new byte[payloadLength];
            buffer.get(payloadBytes);
            payload = new String(payloadBytes, UTF_8_CHARSET);
        } else {
            String msg = MessageFormat.format("Invalid or missing extract format: {0}.", format);
            Log.getInstance().debug(msg);
            throw new IllegalArgumentException(msg);
        }

        if (payload == null) {
            Log.getInstance().debug(MessageFormat.format("Unable to extract payload from carrier: {0}.", carrier));
            return null;
        }
        return payload;
    }

    AdaptiveSampling adaptiveSampling() {
        return adaptiveSampling;
    }

}
