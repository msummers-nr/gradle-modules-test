package com.newrelic.opentracing.dt;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.LambdaSpanContext;
import com.newrelic.opentracing.SpanTestUtils;
import com.newrelic.opentracing.state.DistributedTracingState;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DistributedTracingTest {

    @BeforeClass
    public static void setup() {
        // Here to run tests in IDE. Same values as build.gradle
        Map<String, String> environmentVars = new HashMap<>();
        environmentVars.put("NEW_RELIC_ACCOUNT_ID", "account");
        environmentVars.put("NEW_RELIC_TRUST_KEY", "trustKey");
        environmentVars.put("NEW_RELIC_PRIMARY_APPLICATION_ID", "primaryApp");
        setEnvironmentVariables(environmentVars);
    }

    @Test
    public void testCreatePayloadFromSpan() {
        assertEquals("trustKey", DistributedTracing.INSTANCE.getTrustKey());
        assertEquals("account", DistributedTracing.INSTANCE.getAccountId());
        assertEquals("primaryApp", DistributedTracing.INSTANCE.getApplicationId());

        final LambdaSpan span = SpanTestUtils.createSpan("operation", System.currentTimeMillis(), System.nanoTime(), new HashMap<>(),
                null, "guid", "transactionId");

        final LambdaSpanContext context = (LambdaSpanContext) span.context();
        context.getDistributedTracingState().generateAndStoreTraceId();

        final DistributedTracing distributedTracing = DistributedTracing.INSTANCE;
        final DistributedTracePayloadImpl payload = distributedTracing.createDistributedTracePayload(span);
        assertTrue(payload.timestamp > 0);
        assertEquals("App", payload.parentType);
        assertEquals("account", payload.accountId);
        assertEquals("trustKey", payload.trustKey);
        assertEquals("primaryApp", payload.applicationId);
        assertEquals(span.guid(), payload.guid);
        assertEquals(span.traceId(), payload.traceId);
        assertEquals(span.priority(), payload.priority, 0.0f);
        assertEquals(span.getTransactionId(), payload.txnId);
    }

    @Test
    public void testMajorMinorSupportedVersions() {
        assertEquals(1, DistributedTracing.INSTANCE.getMinorSupportedCatVersion());
        assertEquals(0, DistributedTracing.INSTANCE.getMajorSupportedCatVersion());
    }

    @Test
    public void testIntrinsics() {
        final DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.createDistributedTracePayload("traceId", "guid", "txnId", 0.8f);
        final DistributedTracingState dtState = new DistributedTracingState();
        dtState.setInboundPayloadAndTransportTime(payload, 450);

        final Map<String, Object> dtAtts = DistributedTracing.INSTANCE.getDistributedTracingAttributes(dtState, "someOtherGuid", 1.4f);
        assertEquals("App", dtAtts.get("parent.type"));
        assertEquals("primaryApp", dtAtts.get("parent.app"));
        assertEquals("account", dtAtts.get("parent.account"));
        assertEquals("Unknown", dtAtts.get("parent.transportType"));
        assertEquals(0.45f, (Float) dtAtts.get("parent.transportDuration"), 0.0f);
        assertEquals("someOtherGuid", dtAtts.get("guid"));
        assertEquals("traceId", dtAtts.get("traceId"));
        assertEquals(1.4f, (Float) dtAtts.get("priority"), 0.0f);
    }

    @Test
    public void parsePayload() {
        String strPayload = "{" +
                "  \"v\": [0,1]," +
                "  \"d\": {" +
                "    \"ty\": \"App\"," +
                "    \"ac\": \"account\"," +
                "    \"tk\": \"trustKey\"," +
                "    \"ap\": \"application\"," +
                "    \"id\": \"5f474d64b9cc9b2a\"," +
                "    \"tr\": \"3221bf09aa0bcf0d\"," +
                "    \"pr\": 0.1234," +
                "    \"sa\": false," +
                "    \"ti\": 1482959525577," +
                "    \"tx\": \"27856f70d3d314b7\"" +
                "  }" +
                "}";

        final DistributedTracePayloadImpl payload = DistributedTracePayloadImpl.parseDistributedTracePayload(strPayload);
        assertNotNull(payload);
        assertEquals("trustKey", payload.trustKey);
        assertEquals("account", payload.accountId);
        assertEquals("application", payload.applicationId);
        assertEquals(0.1234f, payload.priority, 0.0f);
        assertEquals("3221bf09aa0bcf0d", payload.traceId);
        assertFalse(payload.sampled);
        assertEquals("App", payload.parentType);
        assertEquals("5f474d64b9cc9b2a", payload.guid);
        assertEquals("27856f70d3d314b7", payload.txnId);
    }

    private static void setEnvironmentVariables(Map<String, String> environmentVariables) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(environmentVariables);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(environmentVariables);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = null;
                    try {
                        field = cl.getDeclaredField("m");
                    } catch (NoSuchFieldException exception) {
                        throw new RuntimeException(exception);
                    }
                    field.setAccessible(true);
                    Object obj;
                    try {
                        obj = field.get(env);
                    } catch (IllegalAccessException exception) {
                        throw new RuntimeException(exception);
                    }
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(environmentVariables);
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
