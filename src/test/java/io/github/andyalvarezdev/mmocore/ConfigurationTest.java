package io.github.andyalvarezdev.mmocore;

import org.junit.After;
import org.junit.Test;

import static java.lang.Math.max;
import static java.lang.Runtime.getRuntime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConfigurationTest {

    @Test
    public void testCorrectConfigurations() {
        System.setProperty("async-mmocore.configurationFile", "/async-mmocore.properties");
        ConnectionConfig<AsyncClient> config = new ConnectionConfig<>(null, null, null);
        config.complete();
        assertTrue(config.bufferPools.size() >= 2);
        assertEquals(0.2f, config.initBufferPoolFactor, 0);
        assertEquals(50 * 1000L, config.shutdownWaitTime);
        assertEquals(6, config.threadPoolSize);

    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonExistentConfigurationFile() {
        System.setProperty("async-mmocore.configurationFile", "/async-mmocore-nonexistent.properties");
        new ConnectionConfig<>(null, null, null);
    }

    @Test
    public void testWrongValuesConfigurations() {
        System.setProperty("async-mmocore.configurationFile", "/async-mmocore-wrong.properties");
        ConnectionConfig<AsyncClient> config = new ConnectionConfig<>(null, null, null);
        config.complete();
        assertEquals(5 * 1000L, config.shutdownWaitTime);
        assertEquals(max(1, getRuntime().availableProcessors() - 2), config.threadPoolSize);
    }

    @After
    public void tearDown() {
        System.setProperty("async-mmocore.configurationFile", "");
    }
}
