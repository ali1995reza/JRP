package jrp.api;

import java.nio.ByteBuffer;

public interface JRPSession {

    void send(ByteBuffer data);

    void close();

    <T> T attachment();

    <T> T attachment(Class<T> cls);

    void attach(Object attachment);
}
