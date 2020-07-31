package io.github.andyalvarezdev.mmocore;

import static java.util.Objects.nonNull;

public class AsyncClient extends Client<Connection<AsyncClient>> {

    public ReadableBuffer receivedPacket;

    public AsyncClient(Connection<AsyncClient> connection) {
        super(connection);
    }

    @Override
    public boolean decrypt(Buffer data, int offset, int size) {
        return true;
    }

    @Override
    public boolean encrypt(Buffer data, int offset, int size) {
        return true;
    }

    @Override
    protected void onDisconnection() {

    }

    @Override
    public void onConnected() {

    }

    public void sendPacket(WritablePacket<AsyncClient> packet) {
        if(nonNull(packet)) {
            System.out.println("Sending " + packet.toString());
        }
        writePacket(packet);
    }
}
