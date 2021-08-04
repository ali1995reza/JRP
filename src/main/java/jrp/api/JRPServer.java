package jrp.api;

public interface JRPServer {

    void start();

    void stop();

    void registerRequestHandler(int requestId, RequestHandler handler);

    void addEventListener(JRPEventListener eventListener);
}
