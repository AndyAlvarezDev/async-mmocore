package io.github.andyalvarezdev.mmocore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

public class Connection<T extends Client<Connection<T>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Connection.class);

    private final AsynchronousSocketChannel channel;
    private final ReadHandler<T> readHandler;
    private final WriteHandler<T> writeHandler;
    private T client;

    private ByteBuffer readingBuffer;
    private ByteBuffer[] writingBuffers;

    Connection(AsynchronousSocketChannel channel, ReadHandler<T> readHandler, WriteHandler<T> writeHandler) {
        this.channel = channel;
        this.readHandler = readHandler;
        this.writeHandler = writeHandler;
    }

    void setClient(T client) {
        this.client = client;
    }

    final void read() {
        if(channel.isOpen()) {
            channel.read(readingBuffer, client, readHandler);
        }
    }

    final void readHeader() {
        if(channel.isOpen()) {
            releaseReadingBuffer();
            readingBuffer = client.getResourcePool().getHeaderBuffer();
            read();
        }
    }

    void read(int size) {
        if(channel.isOpen()) {
            readingBuffer = client.getResourcePool().recycleAndGetNew(readingBuffer, size);
            read();
        }
    }

    final boolean write(ByteBuffer[] buffers) {
        if(!channel.isOpen()) {
            return false;
        }
        writingBuffers = buffers;
        return write();
    }

    final boolean write() {
        if(channel.isOpen() && nonNull(writingBuffers)) {
            channel.write(writingBuffers, 0, writingBuffers.length, -1, TimeUnit.MILLISECONDS,  client, writeHandler);
            return true;
        }
        return false;
    }

    ByteBuffer getReadingBuffer() {
        return readingBuffer;
    }

    private void releaseReadingBuffer() {
        client.getResourcePool().recycleBuffer(readingBuffer);
        readingBuffer=null;
    }

    void releaseWritingBuffer() {
        if(nonNull(writingBuffers)) {
            ResourcePool resourcePool = client.getResourcePool();
            for (ByteBuffer buffer : writingBuffers) {
                resourcePool.recycleBuffer(buffer);
            }
            writingBuffers = null;
        }
    }

    void close() {
        releaseReadingBuffer();
        releaseWritingBuffer();
        try {
            if(channel.isOpen()) {
                channel.close();
            }
        } catch (IOException e) {
            LOGGER.warn(e.getMessage(), e);
        } finally {
            client = null;
        }
    }

    String getRemoteAddress() {
        try {
            InetSocketAddress address = (InetSocketAddress) channel.getRemoteAddress();
            return address.getAddress().getHostAddress();
        } catch (IOException e) {
            return "";
        }
    }

    boolean isOpen() {
        try {
            return channel.isOpen() && nonNull(channel.getRemoteAddress());
        } catch (Exception e) {
            return false;
        }
    }
}
