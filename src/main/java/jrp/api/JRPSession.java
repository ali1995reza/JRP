package jrp.api;

import java.nio.ByteBuffer;

public interface JRPSession {

    void send(ByteBuffer data);

    void close();

    <T> T attachment();

    void attach(Object attachment);
}
