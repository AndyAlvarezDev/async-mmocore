package io.github.andyalvarezdev.mmocore;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Builds the {@link ConnectionHandler} responsible to manage all incoming connections.
 */
public class ConnectionBuilder<T extends Client<Connection<T>>> {

    private ConnectionConfig<T> config;

    /**
     * Creates a ConnectionBuilder holding the minimum requirements to create a ConnectionHandler.
     *
     * @param address - The socket address to listen the incoming connections.
     * @param clientFactory - The factory responsible to create a new Client when a new connection is accepted.
     * @param packetHandler  - The handle responsible to convert the data received into a {@link ReadablePacket}
     * @param executor - The responsible to execute the incoming packets.
     * @param <T> - The Type of client that ConnectionBuilder will handle.
     *
     * @return A ConnectionBuilder with default configuration.
     *
     */
    public static <T extends Client<Connection<T>>> ConnectionBuilder<T> create(InetSocketAddress address, ClientFactory<T> clientFactory, PacketHandler<T> packetHandler, PacketExecutor<T> executor) {
        ConnectionBuilder<T> builder = new ConnectionBuilder<>();
        builder.config = new ConnectionConfig<>(address, clientFactory, new ReadHandler<>(packetHandler, executor));
        return builder;
    }

    /**
     * Sets a filter to be used on incoming connections.
     * The filter must decide if a connection is acceptable or not.
     *
     * @param filter - the {@link ConnectionFilter} to be set.
     *
     * @return  this.
     */
    public ConnectionBuilder<T> filter(ConnectionFilter filter) {
        this.config.acceptFilter = filter;
        return this;
    }

    /**
     * Set the size of the threadPool used to manage the connections and data sending.
     *
     * If the size is less than or equal to zero or greater than {@link Short#MAX_VALUE} then a cachedThreadPool is used.
     * Otherwise a FixedThreadPool with the size set is used.
     *
     * The default value is the quantity of available processors minus 2.
     *
     * @param size - the size to be Set
     *
     * @return this
     */
    public ConnectionBuilder<T> threadPoolSize(int size) {
        this.config.threadPoolSize = size;
        return this;
    }

    /**
     * Defines if small outgoing packets must be combined to be sent all at once. This improves the network performance,
     * but can cause lags on clients waiting for the packet.
     *
     * The default value is false.
     *
     * @param useNagle - true if the Nagle's algorithm must be used.
     *
     * @return this.
     */
    public ConnectionBuilder<T> useNagle(boolean useNagle) {
        this.config.useNagle = useNagle;
        return this;
    }

    /**
     * Sets the shutdown wait time in milliseconds.
     *
     * The default value is 5 seconds.
     *
     * @param waitTime - the wait time to close all connections resources after a {@link ConnectionHandler#shutdown()} is called.
     *
     * @return this
     */
    public ConnectionBuilder<T> shutdownWaitTime(long waitTime) {
        config.shutdownWaitTime = waitTime;
        return this;
    }

    /**
     * Add a new {@link java.nio.ByteBuffer} grouping pool
     *
     * @param size the max amount of {@link java.nio.ByteBuffer} supported
     * @param bufferSize the {@link java.nio.ByteBuffer}'s size inside the pool.
     *
     * @return this
     */
    public ConnectionBuilder<T> addBufferPool(int size, int bufferSize) {
        config.newBufferGroup(size, bufferSize);
        return this;
    }

    /**
     * Define the factor of pre-initialized {@link java.nio.ByteBuffer} inside a pool.
     *
     * @param factor the factor of initialized buffers
     * @return this
     */
    public ConnectionBuilder<T> initBufferPoolFactor(float factor) {
        config.initBufferPoolFactor = factor;
        return this;
    }

    /**
     * Define the size of dynamic buffer's segment. A segment is used to increase the Buffer when needed.
     *
     * @param size of dynamic buffer segment
     * @return this
     */
    public ConnectionBuilder<T> bufferSegmentSize(int size) {
        config.bufferSegmentSize = size;
        return this;
    }

    /**
     * Builds a new ConnectionHandler based on the options configured.
     *
     * @return a ConnectionHandler
     *
     * @throws IOException - If the Socket Address configured can't be used.
     */
    public ConnectionHandler<T> build() throws IOException {
        return new ConnectionHandler<>(config.complete());
    }
}