package io.github.andyalvarezdev.mmocore;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;

public class ClientTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullInitialize() {
        new AsyncClient(null);
    }

    @Test
    public void testRemoteAddress() throws InterruptedException, ExecutionException, IOException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1",9090);
        ConnectionHandler<AsyncClient> handler = ConnectionBuilder.create(socketAddress, AsyncClient::new, (buffer, client) -> null, incomingPacket -> { }).shutdownWaitTime(100).build();
        try {
            handler.start();
            AsyncClient client = Connector.create(AsyncClient::new, ((buffer, client1) -> null), incomingPacket -> {
            }).connect(socketAddress);
            Assert.assertEquals("127.0.0.1", client.getHostAddress());
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testCloseAlreadyClosed() throws InterruptedException, ExecutionException, IOException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1",9090);
        ConnectionHandler<AsyncClient> handler = ConnectionBuilder.create(socketAddress, AsyncClient::new, (buffer, client) -> null, incomingPacket -> { }).shutdownWaitTime(100).build();
        try {
            handler.start();
            AsyncClient client = Connector.create(AsyncClient::new, ((buffer, client1) -> null), incomingPacket -> {
            }).connect(socketAddress);
            client.close(new AsyncClientClosePacket());
            client.close();
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testClosedConnection() throws IOException {
        try(AsynchronousSocketChannel channel = AsynchronousSocketChannel.open()) {
            Connection<AsyncClient> connection = new Connection<>(channel, null, null);
            new AsyncClient(connection);
        }
    }

    @Test
    public void testCloseWithNullPacket() throws InterruptedException, ExecutionException, IOException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1",9090);
        ConnectionHandler<AsyncClient> handler = ConnectionBuilder.create(socketAddress, AsyncClient::new, (buffer, client) -> null, incomingPacket -> { }).shutdownWaitTime(100).build();
        try {
            handler.start();
            AsyncClient client = Connector.create(AsyncClient::new, ((buffer, client1) -> null), incomingPacket -> {
            }).connect(socketAddress);
            client.close();
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testWriteNullPacket() throws IOException, ExecutionException, InterruptedException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 9090);
        ConnectionHandler<AsyncClient> handler = ConnectionBuilder.create(socketAddress, AsyncClient::new, null, null).shutdownWaitTime(100).build();
        try {
            handler.start();
            AsyncClient client = Connector.create(AsyncClient::new, null, null).connect(socketAddress);
            client.sendPacket(null);
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testWriteEmptyPacket() throws IOException, ExecutionException, InterruptedException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 9090);
        ConnectionHandler<AsyncClient> handler = ConnectionBuilder.create(socketAddress, AsyncClient::new, null, null).shutdownWaitTime(100).build();
        try {
            handler.start();
            AsyncClient client = Connector.create(AsyncClient::new, null, null).connect(socketAddress);
            client.sendPacket(new EmptyPacket());
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testWriteEmptyEncryptedPacket() throws IOException, ExecutionException, InterruptedException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 9090);
        ConnectionHandler<AsyncClient> handler = ConnectionBuilder.create(socketAddress, AsyncClient::new, null, null).shutdownWaitTime(100).build();
        try {
            handler.start();
            EmptyEncrypterClient client = Connector.create(EmptyEncrypterClient::new, null, null).connect(socketAddress);
            client.writePacket(new EmptyEncrypterPacket());
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testWriteWithException() throws InterruptedException, ExecutionException, IOException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 9090);
        ConnectionHandler<AsyncClient> handler = ConnectionBuilder.create(socketAddress, AsyncClient::new, null, null).shutdownWaitTime(100).build();
        try {
            handler.start();
            AsyncClient client = Connector.create(AsyncClient::new, null, null).connect(socketAddress);
            client.writePacket(new WritablePacket<>() {
                @Override
                protected boolean write(AsyncClient client) {
                    throw new IllegalStateException();
                }
            });
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testEncriptationFailed() throws InterruptedException, ExecutionException, IOException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1",9090);
        ConnectionHandler<EncriptationFailClient> handler = ConnectionBuilder.create(socketAddress, EncriptationFailClient::new, null, null).shutdownWaitTime(100).build();
        try {
            handler.start();
            EncriptationFailClient client = Connector.create(EncriptationFailClient::new, null, null).connect(socketAddress);
            client.writePacket(new WritablePacket<>() {
                @Override
                protected boolean write(EncriptationFailClient client) {
                    writeLong(90);
                    writeLong(80);
                    return true;
                }
            });
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testEncryptedDataOverflow() throws InterruptedException, IOException, ExecutionException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1",9090);
        ConnectionHandler<BigEncripterClient> handler = ConnectionBuilder.create(socketAddress, BigEncripterClient::new, null, null).shutdownWaitTime(100).build();
        try {
            handler.start();
            BigEncripterClient client = Connector.create(BigEncripterClient::new, null, null)
                    .addBufferPool(10, 4).addBufferPool(10, 16).connect(socketAddress);
            client.writePacket(new WritablePacket<>() {
                @Override
                protected boolean write(BigEncripterClient client) {
                    writeLong(90);
                    writeLong(80);
                    return true;
                }
            });
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    @Test
    public void testNewBuffer() throws InterruptedException, IOException, ExecutionException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1",9090);
        ConnectionHandler<BigEncripterClient> handler = ConnectionBuilder.create(socketAddress, BigEncripterClient::new, null, null).shutdownWaitTime(100).build();
        try {
            handler.start();
            BigEncripterClient client = Connector.create(BigEncripterClient::new, null, null)
                    .addBufferPool(10, 4).addBufferPool(10, 16).connect(socketAddress);
            client.writePacket(new WritablePacket<>() {
                @Override
                protected boolean write(BigEncripterClient client) {
                    writeByte(10);
                    writeShort(20);
                    writeInt(30);
                    writeFloat(40);
                    writeDouble(50);
                    writeBytes((byte)60, (byte)70, (byte) 80);
                    writeLong(90);
                    writeLong(80);
                    return true;
                }
            });
        } finally {
            handler.shutdown();
            handler.join();
        }
    }

    static class BigEncripterClient extends Client<Connection<BigEncripterClient>> {

        public BigEncripterClient(Connection<BigEncripterClient> connection) {
            super(connection);
        }

        public int encryptedSize(int dataSize) {
            return dataSize * 2;
        }

        @Override
        public boolean encrypt(Buffer data, int offset, int size) {
            var encryptedSize = encryptedSize(size);
            data.limit(encryptedSize);
            return true;
        }

        @Override
        public boolean decrypt(Buffer data, int offset, int size) {
            return true;
        }

        @Override
        protected void onDisconnection() {

        }

        @Override
        public void onConnected() {

        }
    }

    static class EncriptationFailClient extends Client<Connection<EncriptationFailClient>> {

        EncriptationFailClient(Connection<EncriptationFailClient> connection) {
            super(connection);
        }

        @Override
        public boolean encrypt(Buffer data, int offset, int size) {
            return false;
        }

        @Override
        public boolean decrypt(Buffer data, int offset, int size) {
            return false;
        }

        @Override
        protected void onDisconnection() {

        }

        @Override
        public void onConnected() {

        }
    }

    static class EmptyEncrypterClient extends Client<Connection<EmptyEncrypterClient>> {

        public EmptyEncrypterClient(Connection<EmptyEncrypterClient> connection) {
            super(connection);
        }

        @Override
        public boolean encrypt(Buffer data, int offset, int size) {
            data.limit(0);
            return true;
        }

        @Override
        public boolean decrypt(Buffer data, int offset, int size) {
            return true;
        }

        @Override
        protected void onDisconnection() {

        }

        @Override
        public void onConnected() {

        }
    }

    static class EmptyEncrypterPacket extends WritablePacket<EmptyEncrypterClient> {

        @Override
        protected boolean write(EmptyEncrypterClient client) {
            writeDouble(42);
            return true;
        }
    }

    static class EmptyPacket extends WritablePacket<AsyncClient> {

        @Override
        protected boolean write(AsyncClient client) {
            return true;
        }
    }
}
