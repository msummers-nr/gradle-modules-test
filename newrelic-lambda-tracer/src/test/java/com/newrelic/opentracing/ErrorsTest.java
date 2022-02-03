package com.newrelic.opentracing;

import com.newrelic.GlobalTracerTestUtils;
import com.newrelic.TestUtils;
import com.newrelic.opentracing.logging.InMemoryLogger;
import com.newrelic.opentracing.logging.Log;
import io.opentracing.Scope;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ErrorsTest {

    @BeforeClass
    public static void beforeClass() {
        GlobalTracerTestUtils.initTracer(LambdaTracer.INSTANCE);
    }

    @Test
    public void testErrors() {
        Log.setInstance(new InMemoryLogger());

        try (Scope scope = GlobalTracer.get().buildSpan("span").startActive(true)) {
            try {
                int x = 1 / 0;
            } catch (Throwable throwable) {
                final Map<String, Object> errorAttributes = new HashMap<>();
                errorAttributes.put("event", Tags.ERROR.getKey());
                errorAttributes.put("error.object", throwable);
                errorAttributes.put("message", throwable.getMessage());
                errorAttributes.put("stack", throwable.getStackTrace());
                errorAttributes.put("error.kind", "Exception");
                scope.span().log(errorAttributes);
            }
        }

        // first log entry is the encoded version of the payload, the second is unencoded we can manually parse / search
        List<String> logs = Log.getInstance().getLogs();
        Assert.assertNotNull(logs);
        Assert.assertEquals(2, logs.size());

        String payload = logs.get(0);
        TestUtils.validateFormat(payload);

        String debugPayload = logs.get(1);
        TestUtils.validateDebugFormat(debugPayload);

        // things in 'error_event_data'
        Assert.assertTrue(debugPayload.contains("{\"events_seen\":1,\"reservoir_size\":1}"));
        Assert.assertTrue(debugPayload.contains("\"error.message\":\"\\/ by zero\""));
        Assert.assertTrue(debugPayload.contains("\"error.class\":\"java.lang.ArithmeticException\""));
        Assert.assertTrue(debugPayload.contains("\"error.kind\":\"Exception\""));

        // things in 'error_data'
        Assert.assertTrue(debugPayload.contains("\"userAttributes\":{\"error.kind\":\"Exception\"}"));
        Assert.assertTrue(debugPayload.contains("\"stack_trace\":[\""));
        Assert.assertTrue(debugPayload.contains("\\tcom.newrelic.opentracing.ErrorsTest.testErrors"));
        Assert.assertTrue(debugPayload.contains("\\tsun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)"));
    }

}
