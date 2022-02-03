package com.newrelic.opentracing.util;

import com.newrelic.opentracing.LambdaSpan;
import com.newrelic.opentracing.events.ErrorEvent;
import com.newrelic.opentracing.events.Event;
import com.newrelic.opentracing.events.TransactionEvent;
import com.newrelic.opentracing.traces.ErrorTrace;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * See the Lambda spec for the Protocol format.
 * https://source.datanerd.us/agents/agent-specs/blob/master/Lambda.md
 */
public class ProtocolUtil {

    private ProtocolUtil() {
    }

    /**
     * Metadata is not compressed or encoded.
     */
    public static Map<String, Object> getMetadata(String arn, String executionEnv) {
        final Map<String, Object> metadata = new HashMap<>();
        metadata.put("protocol_version", 16);
        metadata.put("arn", arn);
        metadata.put("execution_environment", executionEnv);
        metadata.put("agent_version", "1.0.0");
        metadata.put("metadata_version", 2);
        metadata.put("agent_language", "java");
        return metadata;
    }

    public static Map<String, Object> getData(List<LambdaSpan> spans, TransactionEvent transactionEvent, List<ErrorEvent> errorEvents,
            List<ErrorTrace> errorTraces) {
        Map<String, Object> data = new HashMap<>();

        if (spans.size() > 0) {
            addEvents(spans, data, "span_event_data");
        }
        if (transactionEvent != null) {
            addEvents(Collections.singletonList(transactionEvent), data, "analytic_event_data");
        }
        if (errorEvents.size() > 0) {
            addEvents(errorEvents, data, "error_event_data");
        }
        if (errorTraces.size() > 0) {
            data.put("error_data", Arrays.asList(null, errorTraces));
        }

        return data;
    }

    private static void addEvents(List<? extends Event> events, Map<String, Object> data, String eventKey) {
        List<Object> list = new ArrayList<>();
        list.add(0, null);

        final Map<String, Object> eventInfo = new HashMap<>();
        eventInfo.put("events_seen", events.size());
        eventInfo.put("reservoir_size", events.size());
        list.add(1, eventInfo);
        list.add(2, events);
        data.put(eventKey, list);
    }

    /**
     * gzip compress and base64 encode.
     */
    public static String compressAndEncode(String source) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(output);
            gzip.write(source.getBytes(StandardCharsets.UTF_8));
            gzip.flush();
            gzip.close();
            return Base64.encode(output.toByteArray());
        } catch (IOException e) {
        }
        return "";
    }

    /**
     * base64 decode and gzip extract.
     */
    public static String decodeAndExtract(String source) {
        try {
            byte[] bytes = Base64.decode(source);
            ByteArrayInputStream input = new ByteArrayInputStream(bytes);
            GZIPInputStream gzip = new GZIPInputStream(input);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gzip, StandardCharsets.UTF_8));

            String line;
            StringBuilder outStr = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }

            return outStr.toString();
        } catch (IOException e) {
        }
        return "";
    }

}
