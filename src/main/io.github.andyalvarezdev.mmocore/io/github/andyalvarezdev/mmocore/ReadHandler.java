package io.github.andyalvarezdev.mmocore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import static io.github.andyalvarezdev.mmocore.ConnectionConfig.HEADER_SIZE;
import static java.util.Objects.nonNull;

class ReadHandler<T extends Client<Connection<T>>> implements CompletionHandler<Integer, T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReadHandler.class);

    private final PacketHandler<T> packetHandler;
    private final PacketExecutor<T> executor;

    ReadHandler(PacketHandler<T> packetHandler, PacketExecutor<T> executor) {
        this.packetHandler = packetHandler;
        this.executor =  executor;
    }

    @Override
    public void completed(Integer bytesRead, T client) {
        if(!client.isConnected()) {
            return;
        }

        LOGGER.debug("Reading {} from {}", bytesRead, client);
        if(bytesRead < 0 ) {
            client.disconnect();
            return;
        }
        
        if(bytesRead < client.getExpectedReadSize()) {
            client.resumeRead(bytesRead);
            return;
        }

        if(client.isReadingPayload()) {
            handlePayload(client);
        } else {
            handleHeader(client);
        }
    }

    private void handleHeader(T client) {
        ByteBuffer buffer = client.getConnection().getReadingBuffer();
        buffer.flip();
        int dataSize = Short.toUnsignedInt(buffer.getShort()) - HEADER_SIZE;
        client.readPayload(dataSize);
    }

    private void handlePayload(T client) {
        ByteBuffer buffer = client.getConnection().getReadingBuffer();
        buffer.flip();
        parseAndExecutePacket(client, buffer);
        client.read();
    }

    private void parseAndExecutePacket(T client, ByteBuffer incomingBuffer) {
        LOGGER.debug("Trying to parse data");

        try {
            ReadableBuffer buffer = ReadableBuffer.of(incomingBuffer);
            boolean decrypted = client.decrypt(buffer, 0, buffer.remaining());

            if (decrypted) {
                ReadablePacket<T> packet = packetHandler.handlePacket(buffer, client);
                LOGGER.debug("Data parsed to packet {}", packet);
                if (nonNull(packet)) {
                    packet.init(client, buffer);
                    execute(packet);
                }
            }
        } catch (Exception e) {
            LOGGER.warn(e.getMessage(), e);
        }
    }

    private void execute(ReadablePacket<T> packet) {
        if(packet.read()) {
            LOGGER.debug("packet {} was read from client {}", packet, packet.client);
            executor.execute(packet);
        }
     }

    @Override
    public void failed(Throwable e, T client) {
        LOGGER.debug("Failed to read from {}", client, e);
        client.disconnect();
    }
}