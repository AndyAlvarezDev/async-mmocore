package io.github.andyalvarezdev.mmocore;

import io.github.andyalvarezdev.mmocore.internal.SinglePacketBuffer;

import java.nio.ByteBuffer;

public interface ReadableBuffer extends Buffer {

    /**
     * Reads raw <B>byte</B> from the buffer
     * @return byte read
     */
    byte readByte();

    /**
     * Reads <B>short</B> from the buffer. <BR>
     * 16bit integer (00 00)
     * @return short read
     */
    short readShort();

    char readChar();

    /**
     * Reads <B>int</B> from the buffer. <BR>
     * 32bit integer (00 00 00 00)
     * @return int read
     */
    int readInt();

    float readFloat();

    long readLong();

    double readDouble();

    void readBytes(byte[] dst);

    void readBytes(byte[] dst, int offset, int length);

    int remaining();

    static ReadableBuffer of(ByteBuffer buffer) {
        return new SinglePacketBuffer(buffer);
    }
}
