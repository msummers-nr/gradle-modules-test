package com.newrelic.opentracing;

import org.junit.Assert;
import org.junit.Test;

public class AdaptiveSamplingTest {

    @Test
    public void sampleFirstTargetRequests() {
        final AdaptiveSampling sampler = new AdaptiveSampling();
        sampler.requestStarted();

        for (int i = 0; i < sampler.getTarget(); i++) {
            // Sample first target requests
            Assert.assertTrue(sampler.computeSampled());
            sampler.requestStarted();
        }
    }

    @Test
    public void sampleNoMore2xTarget() {
        final AdaptiveSampling sampler = new AdaptiveSampling();
        sampler.requestStarted();

        // warm up sampler
        for (int i = 0; i < sampler.getTarget() * 3; i++) {
            sampler.requestStarted();
            sampler.computeSampled();
        }
        sampler.reset();

        int sampledTrue = 0;
        for (int i = 0; i < 100000; i++) {
            sampler.requestStarted();
            if (sampler.computeSampled()) {
                sampledTrue++;
            }
        }
        final int maxSamples = sampler.getTarget() * 2;
        Assert.assertTrue(sampledTrue <= maxSamples);
    }

}