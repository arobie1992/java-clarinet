package com.github.arobie1992.clarinet.transport;

public interface Handler<I, O> {
    /**
     * Process the incoming message and return the response to send.
     * <p>
     * Return {@code null} if you wish to not send any response.
     * @param message The incoming message.
     * @return The response to send.
     */
    O handle(I message);
    Class<I> inputType();
}
