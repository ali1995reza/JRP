package jrp.api;

import java.nio.ByteBuffer;

public interface JRPRequest {

    int requestId();

    ByteBuffer data();

    void response(int status, ByteBuffer response);

    void response(int status);

    JRPSession requester();

}
