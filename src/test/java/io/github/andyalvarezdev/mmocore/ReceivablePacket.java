package io.github.andyalvarezdev.mmocore;

public class ReceivablePacket extends ReadablePacket<AsyncClient> {

    @Override
    protected boolean read() {
        return true;
    }

    @Override
    public void run() {

    }
}
