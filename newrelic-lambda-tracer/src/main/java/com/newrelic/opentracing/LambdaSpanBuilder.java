package com.newrelic.opentracing;

import com.newrelic.opentracing.state.PrioritySamplingState;
import com.newrelic.opentracing.util.DistributedTraceUtil;
import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LambdaSpanBuilder implements Tracer.SpanBuilder {

    private long startTimeInNanos;
    private boolean ignoreActiveSpan = false;
    private SpanContext parent;

    private final String operationName;
    private final Map<String, Object> tags = new HashMap<>();

    LambdaSpanBuilder(String operationName) {
        this.operationName = operationName;
    }

    @Override
    public Tracer.SpanBuilder asChildOf(SpanContext parent) {
        return addReference(References.CHILD_OF, parent);
    }

    @Override
    public Tracer.SpanBuilder asChildOf(Span parent) {
        return asChildOf(parent.context());
    }

    @Override
    public Tracer.SpanBuilder addReference(String referenceType, SpanContext referencedContext) {
        if (parent == null && (referenceType.equals(References.CHILD_OF) || referenceType.equals(References.FOLLOWS_FROM))) {
            this.parent = referencedContext;
        }
        return this;
    }

    @Override
    public Tracer.SpanBuilder ignoreActiveSpan() {
        this.ignoreActiveSpan = true;
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String key, String value) {
        tags.put(key, value);
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String key, boolean value) {
        tags.put(key, value);
        return this;
    }

    @Override
    public Tracer.SpanBuilder withTag(String key, Number value) {
        tags.put(key, value);
        return this;
    }

    @Override
    public Tracer.SpanBuilder withStartTimestamp(long microseconds) {
        startTimeInNanos = TimeUnit.MICROSECONDS.toNanos(microseconds);
        return this;
    }

    @Override
    public Scope startActive(boolean finishSpanOnClose) {
        return LambdaTracer.INSTANCE.scopeManager().activate(startManual(), finishSpanOnClose);
    }

    @Override
    public Span start() {
        return startManual();
    }

    @Override
    public Span startManual() {
        final LambdaTracer tracer = LambdaTracer.INSTANCE;
        final Span activeSpan = tracer.activeSpan();

        long timestamp = System.currentTimeMillis();
        long startTimeInNanos = this.startTimeInNanos == 0 ? System.nanoTime() : this.startTimeInNanos;

        SpanContext parentSpanContext = null;
        if (!ignoreActiveSpan && parent == null && activeSpan != null) {
            addReference(References.CHILD_OF, activeSpan.context());
            parentSpanContext = activeSpan.context();
        } else if (parent != null) {
            parentSpanContext = parent;
        }

        LambdaScopeManager scopeManager = (LambdaScopeManager) tracer.scopeManager();

        LambdaSpan parentSpan = null;
        if (parentSpanContext instanceof LambdaPayloadContext) {
            final LambdaPayloadContext payloadContext = (LambdaPayloadContext) parentSpanContext;
            scopeManager.dtState.get().setInboundPayloadAndTransportTime(payloadContext.getPayload(), payloadContext.getTransportDurationInMillis());
            scopeManager.dtState.get().setBaggage(payloadContext.getBaggage());
        } else if (parentSpanContext instanceof LambdaSpanContext) {
            parentSpan = ((LambdaSpanContext) parentSpanContext).getSpan();
        }

        LambdaSpan newSpan = new LambdaSpan(operationName, timestamp, startTimeInNanos, tags, parentSpan, DistributedTraceUtil.generateGuid(),
                scopeManager.txnState.get().getTransactionId());
        LambdaSpanContext spanContext = new LambdaSpanContext(newSpan, scopeManager);
        newSpan.setContext(spanContext);

        if (newSpan.isRootSpan()) {
            final AdaptiveSampling adaptiveSampling = LambdaTracer.INSTANCE.adaptiveSampling();
            adaptiveSampling.requestStarted();

            // First span, make a sampling decision and get a traceId
            final PrioritySamplingState pss = spanContext.getPrioritySamplingState();
            pss.setSampledAndGeneratePriority(adaptiveSampling.computeSampled());
            spanContext.getDistributedTracingState().generateAndStoreTraceId();
        }

        return newSpan;
    }

}
