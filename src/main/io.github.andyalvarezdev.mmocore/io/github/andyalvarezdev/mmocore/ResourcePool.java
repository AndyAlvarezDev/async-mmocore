package io.github.andyalvarezdev.mmocore;

import io.github.andyalvarezdev.mmocore.internal.BufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ResourcePool {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePool.class);

    private final ConnectionConfig<?> config;
    private final int[] bufferSizes;

    private ResourcePool(ConnectionConfig<?> config) {
        this.config = config;
        bufferSizes = config.bufferPools.keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
    }

    ByteBuffer getHeaderBuffer() {
        return getSizedBuffer(ConnectionConfig.HEADER_SIZE);
    }

    public ByteBuffer getSegmentBuffer() {
        return getSizedBuffer(config.bufferSegmentSize);
    }

    public ByteBuffer getBuffer(int size) {
        return getSizedBuffer(determineBufferSize(size));
    }

    ByteBuffer recycleAndGetNew(ByteBuffer buffer, int newSize) {
        int bufferSize = determineBufferSize(newSize);
        if(nonNull(buffer)) {
            if(buffer.clear().limit() == bufferSize) {
                return buffer.limit(newSize);
            }
            recycleBuffer(buffer);
        }
        return getSizedBuffer(bufferSize).limit(newSize);
    }

    private ByteBuffer getSizedBuffer(int size) {
        BufferPool pool = config.bufferPools.get(size);
        ByteBuffer buffer = null;
        if(nonNull(pool)) {
            buffer = pool.get();
        }
        if(isNull(buffer)) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.LITTLE_ENDIAN);
        }
        return buffer;
    }

    private int determineBufferSize(int size) {
        for (int bufferSize : bufferSizes) {
            if(size <= bufferSize) {
                return bufferSize;
            }
        }
        LOGGER.warn("There is no buffer pool handling buffer size {}", size);
        return size;
    }

    public void recycleBuffer(ByteBuffer buffer) {
        if (nonNull(buffer)) {
            BufferPool pool = config.bufferPools.get(buffer.capacity());
            if(nonNull(pool)) {
                pool.recycle(buffer);
            }
        }
    }

    static ResourcePool initialize(ConnectionConfig<?> config) {
        return new ResourcePool(config);
    }
}