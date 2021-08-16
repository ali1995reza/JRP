package jrp.impl;

import java.nio.ByteBuffer;

public class ThreadLocalBuffer {

    private final ThreadLocal<ByteBuffer> threadLocal;
    private final boolean direct;
    private final int size;

    public ThreadLocalBuffer(boolean direct, int size) {
        this.direct = direct;
        this.size = size;
        threadLocal = new ThreadLocal<>();
    }

    public ThreadLocalBuffer(int size) {
        this(false, size);
    }

    private ByteBuffer createByteBuffer() {
        if(direct){
            return ByteBuffer.allocateDirect(size);
        } else {
            return ByteBuffer.allocate(size);
        }
    }

    public ByteBuffer getBuffer() {
        ByteBuffer buffer = threadLocal.get();
        if(buffer==null) {
            buffer = createByteBuffer();
            threadLocal.set(buffer);
        }
        buffer.clear();
        return buffer;
    }


}
