package jrp.impl.utils;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class DuplicateWithSize {

    private final int size;
    private final ByteBuffer[] buffers;

    public DuplicateWithSize(int size, ByteBuffer[] buffers) {
        this.size = size;
        this.buffers = buffers;
    }

    public int size() {
        return size;
    }

    public ByteBuffer[] buffers() {
        return buffers;
    }

    public void forEach(Consumer<ByteBuffer> forEach) {
        for (ByteBuffer buffer : buffers) {
            forEach.accept(buffer);
        }
    }

}
