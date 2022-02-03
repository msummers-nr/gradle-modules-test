package com.newrelic.opentracing;

import com.newrelic.opentracing.logging.InMemoryLogger;
import com.newrelic.opentracing.logging.Log;
import com.newrelic.opentracing.util.TimeUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LambdaSpanTest {

    private ByteArrayOutputStream outContent;

    @Before
    public void setupStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    @Test
    public void durationTest() {
        final long start = System.currentTimeMillis();
        final LambdaSpan span = SpanTestUtils.createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid", "txnId");
        sleep(80);
        span.finish();

        final long end = System.currentTimeMillis();
        final long maxExpectedDurationInMs = end - start;

        final float spanDurationInMs = TimeUnit.MICROSECONDS.toMillis(span.getDurationInMicros());
        Assert.assertTrue("Incorrect span duration: " + spanDurationInMs, spanDurationInMs <= maxExpectedDurationInMs);

        final float durationInSec = (float) span.getIntrinsics().get("duration");
        Assert.assertTrue("Incorrect span duration in sec: " + durationInSec, durationInSec <= (maxExpectedDurationInMs / TimeUtil.MILLISECONDS_PER_SECOND));
    }

    @Test
    public void noErrorTag() {
        final long start = System.currentTimeMillis();
        final LambdaSpan span = SpanTestUtils.createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid", "txnId");
        sleep(80);
        span.setTag("error", true);
        span.finish();
        Assert.assertFalse(span.getUserAttributes().containsKey("error"));
    }

    @Test
    public void spanCategory() {
        final long start = System.currentTimeMillis();

        final LambdaSpan span = SpanTestUtils.createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid", "txnId");
        sleep(80);
        span.setTag("http.status_code", "404");
        span.setTag("span.kind", "client");
        span.finish();

        final Map<String, Object> intrinsics = span.getIntrinsics();
        Assert.assertEquals("http", intrinsics.get("category"));
    }

    @Test
    public void spanParenting() {
        final LambdaSpan parent = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), new HashMap<>(), null, "parentGuid", "txnId");

        final LambdaSpan child = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), new HashMap<>(), parent, "childGuid", "txnId");

        final LambdaSpan grandChild = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), new HashMap<>(), child, "grandChildGuid", "txnId");

        final LambdaSpan greatGrandChild = SpanTestUtils.createSpan("operationName", System.currentTimeMillis(),
                System.nanoTime(), new HashMap<>(), grandChild, "greatGrandChild", "txnId");

        Assert.assertNull(parent.getIntrinsics().get("parentId"));
        Assert.assertEquals("parentGuid", child.getIntrinsics().get("parentId"));
        Assert.assertEquals("childGuid", grandChild.getIntrinsics().get("parentId"));
        Assert.assertEquals("grandChildGuid", greatGrandChild.getIntrinsics().get("parentId"));
    }

    @Test
    public void testSampledTrue() {
        Log.setInstance(new InMemoryLogger());

        final long start = System.currentTimeMillis();
        final LambdaSpan span = SpanTestUtils.createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid", "txnId");
        sleep(80);
        span.getPrioritySamplingState().setSampledAndGeneratePriority(true);
        span.finish();

        List<String> logs = Log.getInstance().getLogs();
        final String debugLogEntry = logs.get(1);
        // Should be one Span collected and logged. JSON should contain both the "span_event_data" and "analytic_event_data" hashes.
        Assert.assertTrue(containsEventJsonKey(debugLogEntry, "span_event_data"));
        Assert.assertTrue(containsEventJsonKey(debugLogEntry, "analytic_event_data"));
        outContent.reset();
    }

    @Test
    public void testSampledFalse() {
        Log.setInstance(new InMemoryLogger());

        final long start = System.currentTimeMillis();
        final LambdaSpan span = SpanTestUtils.createSpan("operationName", start, System.nanoTime(), new HashMap<>(), null, "guid", "txnId");
        sleep(80);
        span.getPrioritySamplingState().setSampledAndGeneratePriority(false);
        span.finish();

        List<String> logs = Log.getInstance().getLogs();
        final String debugLogEntry = logs.get(1);
        // Should be no Spans collected and logged. JSON should contain the "analytic_event_data" hash but not the "span_event_data" hash.
        Assert.assertFalse(containsEventJsonKey(debugLogEntry, "span_event_data"));
        Assert.assertTrue(containsEventJsonKey(debugLogEntry, "analytic_event_data"));
        outContent.reset();
    }

    private boolean containsEventJsonKey(String jsonString, String key) {
        JSONParser parser = new JSONParser();
        try {
            // JSONArray with the 4th entry being a JSONObject representing event data
            JSONArray jsonArray = (JSONArray) parser.parse(jsonString);
            final JSONObject eventHash = (JSONObject) jsonArray.get(3);
            return eventHash.containsKey(key);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void sleep(int sleepInMs) {
        final long end = System.currentTimeMillis() + sleepInMs;
        while (System.currentTimeMillis() <= end) {
        }
    }

}