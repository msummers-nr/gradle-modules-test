package com.newrelic.opentracing.dt;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.TransportType;
import com.newrelic.opentracing.state.DistributedTracingState;
import com.newrelic.opentracing.util.DistributedTraceUtil;
import com.newrelic.opentracing.util.TimeUtil;

import java.util.HashMap;
import java.util.Map;

public class DistributedTracing {

    private static final int MAJOR_CAT_VERSION = 0;
    private static final int MINOR_CAT_VERSION = 1;

    private static final String NEW_RELIC_ACCOUNT_ID = "NEW_RELIC_ACCOUNT_ID";
    private static final String NEW_RELIC_TRUST_KEY = "NEW_RELIC_TRUST_KEY";
    private static final String NEW_RELIC_PRIMARY_APPLICATION_ID = "NEW_RELIC_PRIMARY_APPLICATION_ID";

    private final String trustKey;
    private final String accountId;
    private final String primaryAppId;

    private DistributedTracing() {
        this.trustKey = System.getenv(NEW_RELIC_TRUST_KEY);
        this.accountId = System.getenv(NEW_RELIC_ACCOUNT_ID);
        this.primaryAppId = System.getenv(NEW_RELIC_PRIMARY_APPLICATION_ID);
    }

    public static final DistributedTracing INSTANCE = new DistributedTracing();

    int getMajorSupportedCatVersion() {
        return MAJOR_CAT_VERSION;
    }

    int getMinorSupportedCatVersion() {
        return MINOR_CAT_VERSION;
    }

    String getAccountId() {
        return accountId;
    }

    String getApplicationId() {
        return primaryAppId;
    }

    public Map<String, Object> getDistributedTracingAttributes(DistributedTracingState dtState, String guid, float priority) {
        Map<String, Object> attributes = new HashMap<>();

        final DistributedTracePayloadImpl inboundPayload = dtState.getInboundPayload();
        if (inboundPayload != null) {
            if (inboundPayload.parentType != null) {
                attributes.put("parent.type", inboundPayload.parentType);
            }
            if (inboundPayload.applicationId != null) {
                attributes.put("parent.app", inboundPayload.applicationId);
            }
            if (inboundPayload.accountId != null) {
                attributes.put("parent.account", inboundPayload.accountId);
            }

            // Record unknown for now. There's no good way of identifying transport type in OpenTracing
            attributes.put("parent.transportType", TransportType.Unknown.name());

            final long transportDurationInMillis = dtState.getTransportTimeMillis();
            if (transportDurationInMillis >= 0) {
                float transportDurationSec = transportDurationInMillis / TimeUtil.MILLISECONDS_PER_SECOND;
                attributes.put("parent.transportDuration", transportDurationSec);
            }
        }

        attributes.put("guid", guid);
        attributes.put("traceId", dtState.getTraceId());
        attributes.put("priority", priority);
        attributes.put("sampled", DistributedTraceUtil.isSampledPriority(priority));

        return attributes;
    }

    String getTrustKey() {
        return trustKey;
    }

    public DistributedTracePayloadImpl createDistributedTracePayload(LambdaSpan span) {
        return DistributedTracePayloadImpl.createDistributedTracePayload(span.traceId(), span.guid(), span.getTransactionId(), span.priority());
    }

}
