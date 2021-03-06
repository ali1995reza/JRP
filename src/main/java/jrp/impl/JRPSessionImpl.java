package jrp.impl;

import jrp.api.JRPEventListener;
import jrp.api.JRPSession;
import jrp.api.ProtocolConstants;
import jrp.impl.utils.BufferUtils;
import jrp.impl.utils.DuplicateWithSize;
import jrp.utils.transport.nio.model.IoContext;
import jrp.utils.transport.nio.model.IoHandler;
import jrp.utils.transport.nio.model.IoOperation;
import jrp.utils.transport.nio.model.IoState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.function.BiConsumer;

public class JRPSessionImpl implements JRPSession, IoHandler {

    private final static ByteBuffer EMPTY_PACKET = ByteBuffer.allocate(0);
    private final static ByteBuffer MESSAGE_HEADER = ByteBuffer.wrap(new byte[]{ProtocolConstants.MESSAGE});

    private final SocketChannel socketChannel;
    private final JRPEventListener eventListener;
    private final ByteBuffer packetDetector = ByteBuffer.allocate(4);
    private ByteBuffer currentPacket;
    private ByteBuffer sendBuffer = ByteBuffer.allocateDirect(10240);
    private IoState ioState;
    private Object attachment;
    private boolean idle;
    private final JRPSessionDetailsHolder sessionDetails = new JRPSessionDetailsHolder();
    private final BiConsumer<JRPSessionImpl, ByteBuffer> packetHandler;

    public JRPSessionImpl(SocketChannel socketChannel, JRPEventListener eventListener, BiConsumer<JRPSessionImpl, ByteBuffer> packetHandler) {
        this.socketChannel = socketChannel;
        this.eventListener = eventListener;
        this.packetHandler = packetHandler;
    }


    public void sendRaw(ByteBuffer ... buffers) {
        DuplicateWithSize data = BufferUtils.duplicate(buffers);
        sendRaw(data);
    }

    private void sendRaw(DuplicateWithSize data) {
        synchronized (sendBuffer) {
            if (data.size() + 4 <= sendBuffer.remaining()) {
                sendBuffer.putInt(data.size());
                data.forEach(sendBuffer::put);
                ioState.doReadAndWrite();
            } else {
                throw new IllegalStateException("buffer is full");
            }
        }
    }

    @Override
    public void send(ByteBuffer ... buffers) {
        DuplicateWithSize duplicate = BufferUtils.duplicate(MESSAGE_HEADER, buffers);
        sendRaw(duplicate);
    }

    @Override
    public void close() {
        ioState.cancel();
    }

    @Override
    public <T> T attachment() {
        return (T) attachment;
    }

    @Override
    public <T> T attachment(Class<T> cls) {
        return attachment();
    }

    @Override
    public void attach(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public void onRegister(IoContext context, IoState state) {
        ioState = state;
        ioState.doRead();
        ioState.setOperationsToCheckIdle(IoOperation.WRITE, IoOperation.READ);
        try {
            eventListener.onSessionOpened(this);
        } catch (Exception e) {
            ioState.cancel();
        }
    }

    @Override
    public void onCanceled(IoContext context) {
        try {
            socketChannel.close();
        } catch (IOException e) {
        }
        eventListener.onSessionClosed(this);
    }

    @Override
    public void read(IoContext context) throws IOException {
        //todo read a message here !
        if (currentPacket == null) {
            int read = socketChannel.read(packetDetector);
            if (read <= 0)
                throw new IOException("socket input closed");
            idle = false;
            if (!packetDetector.hasRemaining()) {
                packetDetector.position(0);
                int packetSize = packetDetector.getInt();
                packetDetector.clear();
                if (packetSize < 0)
                    throw new IOException("packet size negative");
                if (packetSize > 0) {
                    currentPacket = ByteBuffer.allocate(packetSize);
                }
            }
        } else {
            int read = socketChannel.read(currentPacket);
            if (read <= 0)
                throw new IOException("socket input closed");

            if(!currentPacket.hasRemaining()) {
                //packet received ! todo handle it please !
                currentPacket.flip();
                ByteBuffer pack = currentPacket;
                currentPacket = null;
                packetHandler.accept(this, pack);
            }
        }
    }

    @Override
    public void write(IoContext context) throws IOException {
        synchronized (sendBuffer) {
            sendBuffer.flip();
            int wrote = socketChannel.write(sendBuffer);
            if (wrote <= 0)
                throw new IOException("output closed");

            if (!sendBuffer.hasRemaining()) {
                sendBuffer.clear();
                ioState.doRead();
            } else {
                sendBuffer.compact();
            }
        }
    }

    @Override
    public void accept(IoContext context) throws IOException {

    }

    @Override
    public void connect(IoContext context) throws IOException {

    }

    @Override
    public void onIdle(IoContext context, IoOperation operation) throws IOException {
        //todo handle it please !
        if (operation == IoOperation.READ) {
            if (idle)
                throw new IOException("socket idle");
            else idle = true;
        } else {
            sendRaw(EMPTY_PACKET);
        }
    }

    @Override
    public void onException(Throwable e, IoOperation op) {
        e.printStackTrace();
        if (!ioState.isCanceled()) {
            ioState.cancel();
        }
    }

    @Override
    public SelectableChannel channel() {
        return socketChannel;
    }

    public JRPSessionDetailsHolder sessionDetails() {
        return sessionDetails;
    }
}
