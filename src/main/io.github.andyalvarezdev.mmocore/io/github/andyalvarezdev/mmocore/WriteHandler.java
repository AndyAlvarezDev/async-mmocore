package io.github.andyalvarezdev.mmocore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

class WriteHandler<T extends Client<Connection<T>>> implements CompletionHandler<Long, T> {

    private  static final Logger LOGGER = LoggerFactory.getLogger(WriteHandler.class);

    @Override
    public void completed(Long result, T client) {
        if(result < 0) {
            LOGGER.warn("Couldn't send data to client {}", client);
            if(client.isConnected()) {
                client.disconnect();
            }
            return;
        }

        if(result < client.getDataSentSize() && result > 0) {
            LOGGER.debug("Still {} data to send. Trying to send", result);
            client.resumeSend(result);
        } else {
            client.finishWriting();
        }
    }

    @Override
    public void failed(Throwable e, T client) {
        if(! (e instanceof IOException)) {
            LOGGER.warn(e.getMessage(), e);
        }
        client.disconnect();
    }
}