package io.github.andyalvarezdev.mmocore;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.fail;

public class CommunicationTest {

    private static final AtomicBoolean shutdown = new AtomicBoolean(false);
    private static boolean success = true;
    private ConnectionBuilder<AsyncClient> builder;
    private Connector<AsyncClient> connector;
    private final InetSocketAddress listenAddress = new InetSocketAddress(9090);
    private ConnectionHandler<AsyncClient> connectionHandler;

    static void shutdown(boolean success) {
        shutdown.getAndSet(true);
        CommunicationTest.success = success;
    }

    @Before
    public void SetUp() {
        GenericClientHandler handler = new GenericClientHandler();

        builder = ConnectionBuilder.create(listenAddress, AsyncClient::new, handler, handler).filter(channel -> true).threadPoolSize(2).useNagle(false)
                .shutdownWaitTime(500).addBufferPool(10,300).initBufferPoolFactor(0.2f).bufferSegmentSize(256);
        connector = Connector.create(AsyncClient::new, handler, handler).addBufferPool(10, 300).initBufferPoolFactor(0.2f).bufferSegmentSize(128);

    }

    @Test
    public void testIntegration() throws IOException, ExecutionException, InterruptedException {
        connectionHandler = builder.build();
        connectionHandler.start();

        AsyncClient client = connector.connect("localhost", 9090);
        client.sendPacket(new AsyncClientPingPacket());

        Awaitility.waitAtMost(10, TimeUnit.SECONDS).untilTrue(shutdown);

        connectionHandler.shutdown();
        connectionHandler.join();
        if(!success) {
            fail();
        }
    }

    @After
    public void tearDown() {
        if(!shutdown.get()) {
            connectionHandler.shutdown();
        }
    }
}
