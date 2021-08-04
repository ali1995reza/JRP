package jrp.impl;

import jrp.api.JRPEventListener;
import jrp.utils.transport.nio.model.IoContext;
import jrp.utils.transport.nio.model.IoHandler;
import jrp.utils.transport.nio.model.IoOperation;
import jrp.utils.transport.nio.model.IoState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.BiConsumer;

public class JRPSocketServerIoHandler implements IoHandler {

    private final ServerSocketChannel serverSocketChannel;
    private final JRPEventListener eventListener;
    private final BiConsumer<JRPSessionImpl, ByteBuffer> packetHander;

    public JRPSocketServerIoHandler(InetSocketAddress address, JRPEventListener eventListener, BiConsumer<JRPSessionImpl, ByteBuffer> packetHander) {
        this.eventListener = eventListener;
        this.packetHander = packetHander;
        try {
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.bind(address);
            this.serverSocketChannel.configureBlocking(false);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onRegister(IoContext context, IoState state) {
        state.doAccept();
    }

    @Override
    public void onCanceled(IoContext context) {

    }

    @Override
    public void read(IoContext context) throws IOException {

    }

    @Override
    public void write(IoContext context) throws IOException {

    }

    @Override
    public void accept(IoContext context) throws IOException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        JRPSessionImpl session = new JRPSessionImpl(
                socketChannel,
                eventListener,
                packetHander
        );
        context.processor().registerIoHandler(session);
    }

    @Override
    public void connect(IoContext context) throws IOException {

    }

    @Override
    public void onIdle(IoContext context, IoOperation operation) throws IOException {

    }

    @Override
    public void onException(Throwable e, IoOperation op) {

    }

    @Override
    public SelectableChannel channel() {
        return serverSocketChannel;
    }
}
