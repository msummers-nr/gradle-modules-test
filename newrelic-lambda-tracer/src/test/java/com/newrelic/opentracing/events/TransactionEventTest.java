package com.newrelic.opentracing.events;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.SpanTestUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransactionEventTest {

    @Test
    public void testTransactionEvent() {
        Map<String, Object> tags = new HashMap<>();
        tags.put("world", "hello");
        tags.put("one", 1);
        tags.put("tagThree", "value");

        final LambdaSpan span = SpanTestUtils.createSpan("fetch", System.currentTimeMillis(), System.nanoTime(), tags, null, "guid", "txnId");
        final TransactionEvent transactionEvent = new TransactionEvent(span);

        assertEquals("hello", transactionEvent.getUserAttributes().get("world"));
        assertEquals(1, transactionEvent.getUserAttributes().get("one"));
        assertEquals("value", transactionEvent.getUserAttributes().get("tagThree"));

        assertNotNull(transactionEvent.getIntrinsics().get("duration"));
        assertNotNull(transactionEvent.getIntrinsics().get("guid"));
        assertNotNull(transactionEvent.getIntrinsics().get("type"));
        assertNotNull(transactionEvent.getIntrinsics().get("sampled"));
        assertNotNull(transactionEvent.getIntrinsics().get("timestamp"));
        assertNotNull(transactionEvent.getIntrinsics().get("priority"));
    }

}
