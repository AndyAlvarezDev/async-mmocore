package io.github.andyalvarezdev.mmocore.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BufferPool {

    private final Queue<ByteBuffer> buffers = new ConcurrentLinkedQueue<>();
    private final int maxSize;
    private final int bufferSize;

    public BufferPool(int maxSize, int bufferSize) {
        this.maxSize = maxSize;
        this.bufferSize = bufferSize;
    }
    public void initialize(float factor) {
        final int amount = (int) Math.min(maxSize, maxSize * factor);
        for (int i = 0; i < amount; i++) {
            buffers.offer(ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.LITTLE_ENDIAN));
        }
    }

    public void recycle(ByteBuffer buffer) {
        if(buffers.size() < maxSize) {
            buffers.offer(buffer.clear());
        }
    }

    public ByteBuffer get() {
        return buffers.poll();
    }
}
