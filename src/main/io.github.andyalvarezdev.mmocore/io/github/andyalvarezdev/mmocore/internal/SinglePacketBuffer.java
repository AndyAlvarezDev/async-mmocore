package io.github.andyalvarezdev.mmocore.internal;

import io.github.andyalvarezdev.mmocore.ReadableBuffer;

import java.nio.ByteBuffer;

public class SinglePacketBuffer implements ReadableBuffer {

    private final ByteBuffer buffer;

    public SinglePacketBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public byte readByte() {
        return buffer.get();
    }

    @Override
    public short readShort() {
        return buffer.getShort();
    }

    @Override
    public char readChar() {
        return buffer.getChar();
    }

    @Override
    public int readInt() {
        return buffer.getInt();
    }

    @Override
    public float readFloat() {
        return buffer.getFloat();
    }

    @Override
    public long readLong() {
        return buffer.getLong();
    }

    @Override
    public double readDouble() {
        return buffer.getDouble();
    }

    @Override
    public void readBytes(byte[] dst) {
        buffer.get(dst);
    }

    @Override
    public void readBytes(byte[] dst, int offset, int length) {
        buffer.get(dst, offset, length);
    }

    @Override
    public int remaining() {
        return buffer.remaining();
    }

    @Override
    public byte readByte(int index) {
        return buffer.get(index);
    }

    @Override
    public void writeByte(int index, byte value) {
        buffer.put(index, value);
    }

    @Override
    public short readShort(int index) {
        return buffer.getShort(index);
    }

    @Override
    public void writeShort(int index, short value) {
        buffer.putShort(index, value);
    }

    @Override
    public int limit() {
        return buffer.limit();
    }

    @Override
    public void limit(int newLimit) {
        buffer.limit(newLimit);
    }

    @Override
    public int readInt(int index) {
        return buffer.getInt(index);
    }

    @Override
    public void writeInt(int index, int value) {
        buffer.putInt(index, value);
    }

}
