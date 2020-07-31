package io.github.andyalvarezdev.mmocore;

public class SendablePacket extends WritablePacket<AsyncClient> {

    @Override
    protected boolean write(AsyncClient client) {
        return true;
    }
}