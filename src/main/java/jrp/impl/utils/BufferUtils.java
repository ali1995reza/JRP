package jrp.impl.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class BufferUtils {


    public static DuplicateWithSize duplicate(ByteBuffer... buffers) {
        ByteBuffer[] duplicate = new ByteBuffer[buffers.length];
        int size = 0;
        for (int i = 0; i < buffers.length; i++) {
            duplicate[i] = buffers[i].duplicate();
            size += duplicate[i].remaining();
        }

        return new DuplicateWithSize(size, duplicate);
    }

    public static DuplicateWithSize duplicate(ByteBuffer header, ByteBuffer... buffers) {
        ByteBuffer[] duplicate = new ByteBuffer[buffers.length+1];
        int size = 0;
        duplicate[0] = header.duplicate();
        size += duplicate[0].remaining();
        for (int i = 0; i < buffers.length; i++) {
            duplicate[i+1] = buffers[i].duplicate();
            size += duplicate[i+1].remaining();
        }

        return new DuplicateWithSize(size, duplicate);
    }


    public static String getString(ByteBuffer buffer, int len, Charset charset) {
        byte[] data = new byte[len];
        buffer.get(data);
        return new String(data, charset);
    }

    public static String getString(ByteBuffer buffer, int len){
        return getString(buffer, len, StandardCharsets.UTF_8);
    }

    public static String getString(ByteBuffer buffer, Charset charset) {
        return getString(buffer, buffer.remaining(), charset);
    }

    public static String getString(ByteBuffer buffer) {
        return getString(buffer, StandardCharsets.UTF_8);
    }
}
