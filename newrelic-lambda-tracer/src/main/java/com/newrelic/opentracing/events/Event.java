package com.newrelic.opentracing.events;

import org.json.simple.JSONArray;
import org.json.simple.JSONAware;

import java.util.Arrays;
import java.util.Map;

public abstract class Event implements JSONAware {

    public abstract Map<String, Object> getIntrinsics();

    public abstract Map<String, Object> getUserAttributes();

    public abstract Map<String, Object> getAgentAttributes();

    /**
     * Print an event according to the P16 data format, which is an array of 3 hashes representing intrinsics,
     * user attributes, and agent attributes.
     */
    @Override
    public String toString() {
        return toJSONString();
    }

    @Override
    public String toJSONString() {
        return JSONArray.toJSONString(Arrays.asList(getIntrinsics(), getUserAttributes(), getAgentAttributes()));
    }

}
