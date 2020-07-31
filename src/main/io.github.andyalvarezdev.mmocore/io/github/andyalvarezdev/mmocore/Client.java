package io.github.andyalvarezdev.mmocore;

import io.github.andyalvarezdev.mmocore.internal.WritableBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.andyalvarezdev.mmocore.ConnectionConfig.HEADER_SIZE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public abstract class Client<T extends Connection<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    private final T connection;
    private final Queue<WritablePacket<? extends Client<T>>> packetsToWrite = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean writing = new AtomicBoolean(false);
    private int dataSentSize;
    private volatile boolean isClosing;
    private ResourcePool resourcePool;
    private boolean readingPayload;
    private int expectedReadSize;

    /**
     * Construct a new Client
     *
     * @param connection - The Connection to the client.
     * @throws IllegalArgumentException if the connection is null or closed.
     */
    public Client(T connection) {
        if(isNull(connection) || !connection.isOpen()) {
            throw new IllegalArgumentException("The Connection is null or closed");
        }
        this.connection = connection;
    }

    /**
     * Sends a packet to this client.
     *
     * If another packet is been sent to this client, the actual packet is put on a queue to be sent after all previous packets.
     * Otherwise the packet is sent immediately.
     *
     * @param packet to be sent.
     */
    protected final void writePacket(WritablePacket<? extends Client<T>> packet) {
        if(!isConnected() || isNull(packet)) {
            return;
        }
        packetsToWrite.add(packet);
        tryWriteNextPacket();
    }

    private void tryWriteNextPacket() {
        if(writing.compareAndSet(false, true)) {
            if(packetsToWrite.isEmpty()) {
                connection.releaseWritingBuffer();
                writing.set(false);
                LOGGER.debug("There is no packet to send");
                if(isClosing) {
                    disconnect();
                }
            } else {
                write(packetsToWrite.poll());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void write(WritablePacket packet) {
        boolean sendedData = false;
        try {
            WritableBuffer data = packet.writeData(this);

            if(isNull(data)) {
                return;
            }

            var payloadSize = data.limit() - HEADER_SIZE;
            if(payloadSize <= 0) {
                return;
            }

            if(encrypt(data, HEADER_SIZE, payloadSize)) {
                dataSentSize = data.limit();

                if (dataSentSize <= HEADER_SIZE) {
                    return;
                }

                packet.writeHeaderAndRecord(dataSentSize);
                sendedData = connection.write(data.toByteBuffers());
                LOGGER.debug("Sending packet {}[{}] to {}", packet, dataSentSize, this);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            WritableBuffer writable = packet.releaseData();
            if(!sendedData) {
                connection.releaseWritingBuffer();
                writable.releaseResources();
                finishWriting();
            }
        }
    }

    void read() {
        expectedReadSize = HEADER_SIZE;
        readingPayload = false;
        connection.readHeader();
    }

    void readPayload(int dataSize) {
        expectedReadSize = dataSize;
        readingPayload = true;
        connection.read(dataSize);
    }

    /**
     * close the underlying Connection to the client.
     *
     * All pending packets are cancelled.
     *
     */
    public void close() {
        close(null);
    }

    /**
     * Sends the packet and close the underlying Connection to the client.
     *
     * All others pending packets are cancelled.
     *
     * @param packet to be sent before the connection is closed.
     */
    public void close(WritablePacket<? extends Client<T>> packet) {
        if(!isConnected()) {
            return;
        }
        packetsToWrite.clear();
        if(nonNull(packet)) {
            packetsToWrite.add(packet);
        }
        isClosing = true;
        LOGGER.debug("Closing client connection {} with packet {}", this, packet);
        tryWriteNextPacket();
    }

    void resumeSend(long result) {
        dataSentSize-= result;
        connection.write();
    }

    void finishWriting() {
        connection.releaseWritingBuffer();
        writing.set(false);
        tryWriteNextPacket();
    }

    final void disconnect() {
        LOGGER.debug("Client {} disconnecting", this);
        onDisconnection();
        try {
            // Give a time to send last packet
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            connection.close();
        }
    }

    T getConnection() {
        return connection;
    }

    int getDataSentSize() {
        return dataSentSize;
    }

    /**
     * @return The client's IP address.
     */
    public String getHostAddress() {
        return connection.getRemoteAddress();
    }

    /**
     * @return if client still connected
     */
    public boolean isConnected() {
        return connection.isOpen() && !isClosing;
    }

    void setResourcePool(ResourcePool resourcePool) {
        this.resourcePool = resourcePool;
    }

    ResourcePool getResourcePool() {
        return resourcePool;
    }

    boolean isReadingPayload() {
        return readingPayload;
    }

    void resumeRead(int bytesRead) {
        expectedReadSize -=  bytesRead;
        connection.read();
    }

    int getExpectedReadSize() {
        return expectedReadSize;
    }

    /**
     * Encrypt the data in-place.
     * @param data - the data to be encrypted
     * @param offset - the initial index to be encrypted
     * @param size - the length of data to be encrypted
     *
     * @return if data was encrypted
     */
    public abstract boolean encrypt(Buffer data, int offset, int size);

    /**
     * Decrypt the data in-place
     *
     * @param data - data to be decrypted
     * @param offset - the initial index to be encrypted.
     * @param size - the length of data to be encrypted.
     *
     * @return if the data was decrypted.
     */
    public abstract boolean decrypt(Buffer data, int offset, int size);

    /**
     * Handles the client's disconnection.
     *
     * This method must save all data and release all resources related to the client.
     *
     * No more packet can be sent after this method is called.
     */
    protected abstract void  onDisconnection();

    /**
     * Handles the client's connection.
     * This method should not use blocking operations.
     *
     * The Packets can be sent only after this method is called.
     */
    public abstract void onConnected();
}
