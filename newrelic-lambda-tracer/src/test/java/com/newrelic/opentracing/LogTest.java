package com.newrelic.opentracing;

import com.newrelic.opentracing.logging.ConsoleLogger;
import com.newrelic.opentracing.logging.DebugLogger;
import com.newrelic.opentracing.logging.InMemoryLogger;
import com.newrelic.opentracing.logging.Log;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public class LogTest {

    private ByteArrayOutputStream outContent;

    @Before
    public void setupStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
    }

    /**
     * Only 'out' should have any logs.
     */
    @Test
    public void testConsoleLogger() throws IOException {
        Log.setInstance(new ConsoleLogger());

        Log.getInstance().out("test");
        Assert.assertTrue(outContent.toString().equals("test\n"));
        outContent.reset();

        Log.getInstance().debug("debug");
        Assert.assertTrue(outContent.toString().equals(""));
        outContent.reset();

        List<String> logs = Log.getInstance().getLogs();
        Assert.assertNotNull(logs);
        Assert.assertEquals(0, logs.size());
    }

    /**
     * Both 'out' and 'debug' should have logs.
     */
    @Test
    public void testDebugLogger() throws IOException {
        Log.setInstance(new DebugLogger());

        Log.getInstance().out("test");
        Assert.assertTrue(outContent.toString().equals("test\n"));
        outContent.reset();

        Log.getInstance().debug("debug");
        Assert.assertTrue(outContent.toString().equals("nr_debug: debug\n"));
        outContent.reset();

        List<String> logs = Log.getInstance().getLogs();
        Assert.assertNotNull(logs);
        Assert.assertEquals(0, logs.size());
    }

    /**
     * Neither 'out' nor 'debug' should have logs.
     */
    @Test
    public void testInMemoryLogger() throws IOException {
        Log.setInstance(new InMemoryLogger());

        Log.getInstance().out("test");
        Assert.assertTrue(outContent.toString().equals(""));
        outContent.reset();

        Log.getInstance().debug("debug");
        Assert.assertTrue(outContent.toString().equals(""));
        outContent.reset();

        List<String> logs = Log.getInstance().getLogs();
        Assert.assertNotNull(logs);
        Assert.assertEquals(2, logs.size());
        Assert.assertTrue("test".equals(logs.get(0)));
        Assert.assertTrue("debug".equals(logs.get(1)));
    }

}
