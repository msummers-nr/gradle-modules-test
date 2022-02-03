package com.newrelic;

import com.newrelic.opentracing.util.ProtocolUtil;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;

import java.util.List;
import java.util.Map;

public class TestUtils {

    /**
     * Validate the metadata section of the payload is parseable.
     *
     * There are 6 metadata attributes, but 'execution_environment' and 'arn' won't be present in tests.
     */
    private static void validateMetaData(Map<String, Object> metadata) {
        Assert.assertNotNull(metadata);
        Assert.assertEquals(6, metadata.size());
        Assert.assertNotNull(metadata.get("agent_version"));
        Assert.assertNotNull(metadata.get("protocol_version"));
        Assert.assertNotNull(metadata.get("agent_language"));
        Assert.assertNotNull(metadata.get("metadata_version"));
    }

    /**
     * Validate the data section of the payload is parseable.
     */
    private static void validateData(Map<String, Object> data) {
        Assert.assertNotNull(data);

        List<Object> spanEventData = (List<Object>) data.get("span_event_data");
        Assert.assertNotNull(spanEventData);
        Assert.assertEquals(3, spanEventData.size());

        List<Object> errorEventData = (List<Object>) data.get("error_event_data");
        Assert.assertNotNull(errorEventData);
        Assert.assertEquals(3, errorEventData.size());

        List<Object> analyticEventData = (List<Object>) data.get("analytic_event_data");
        Assert.assertNotNull(analyticEventData);
        Assert.assertEquals(3, analyticEventData.size());

        List<Object> errorData = (List<Object>) data.get("error_data");
        Assert.assertNotNull(errorData);
        Assert.assertEquals(2, errorData.size());
    }

    /**
     * Validate the debug JSON format of the payload that is logged.
     */
    public static void validateDebugFormat(String payload) {
        try {
            JSONParser parser = new JSONParser();
            JSONArray object = (JSONArray) parser.parse(payload);

            Long version = (Long) object.get(0);
            Assert.assertTrue(version == 2);

            String debug = (String) object.get(1);
            Assert.assertTrue(debug.equals("DEBUG"));

            Map<String, Object> metadata = (Map<String, Object>) object.get(2);
            validateMetaData(metadata);

            Map<String, Object> data = (Map<String, Object>) object.get(3);
            validateData(data);
        } catch (ParseException e) {
            Assert.fail();
        }
    }

    /**
     * Validate the JSON format of the payload that is logged.
     */
    public static void validateFormat(String payload) {
        try {
            JSONParser parser = new JSONParser();
            JSONArray object = (JSONArray) parser.parse(payload);

            Long version = (Long) object.get(0);
            Assert.assertTrue(version == 2);

            String debug = (String) object.get(1);
            Assert.assertTrue(debug.equals("NR_LAMBDA_MONITORING"));

            Map<String, Object> metadata = (Map<String, Object>) object.get(2);
            validateMetaData(metadata);

            String dataString = (String) object.get(3);
            Assert.assertNotNull(dataString);

            String decodedDataString = ProtocolUtil.decodeAndExtract(dataString);
            Map<String, Object> data = (Map<String, Object>) parser.parse(decodedDataString);
            validateData(data);
        } catch (ParseException e) {
            Assert.fail();
        }
    }

}
