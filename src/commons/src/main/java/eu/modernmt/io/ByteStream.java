package eu.modernmt.io;

import eu.modernmt.lang.Language;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;

public class ByteStream {

    private ByteBuffer buffer;

    public ByteStream() {
        this(32);
    }

    public ByteStream(int initialCapacity) {
        buffer = ByteBuffer.allocate(initialCapacity);
    }

    public ByteStream(byte[] array) {
        buffer = ByteBuffer.wrap(array);
    }

    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buffer.capacity() > 0)
            grow(minCapacity);
    }

    public byte[] toArray() {
        byte[] result = new byte[buffer.position()];
        System.arraycopy(buffer.array(), 0, result, 0, result.length);
        return result;
    }

    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buffer.capacity();
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);

        int position = buffer.position();
        buffer = ByteBuffer.wrap(Arrays.copyOf(buffer.array(), newCapacity));
        buffer.position(position);
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    // Read

    public byte readByte() {
        return buffer.get();
    }

    public long readLong() {
        return buffer.getLong();
    }

    public UUID readUUID() {
        if (buffer.remaining() == 0)
            return null;

        long msb = buffer.getLong();
        long lsb = buffer.getLong();

        return (msb == 0L && lsb == 0L) ? null : new UUID(msb, lsb);
    }

    public String readString() {
        if (buffer.remaining() == 0) return null;

        int length = buffer.getInt();
        if (length == 0) return null;

        String string = new String(buffer.array(), buffer.position(), length, UTF8Charset.get());
        buffer.position(buffer.position() + length);
        return string;
    }

    public Date readDate() {
        if (buffer.remaining() == 0)
            return null;
        long millis = buffer.getLong();
        return millis == 0L ? null : new Date(millis);
    }

    public Language readLanguage() {
        String value = readString();
        return value == null ? null : Language.fromString(value);
    }

    // Write

    public ByteStream write(byte value) {
        ensureCapacity(buffer.position() + 1);
        buffer.put(value);
        return this;
    }

    public ByteStream write(long value) {
        ensureCapacity(buffer.position() + 8);
        buffer.putLong(value);
        return this;
    }

    public ByteStream write(UUID value) {
        ensureCapacity(buffer.position() + 16);

        long msb = value == null ? 0L : value.getMostSignificantBits();
        long lsb = value == null ? 0L : value.getLeastSignificantBits();

        buffer.putLong(msb);
        buffer.putLong(lsb);

        return this;
    }

    public ByteStream write(String value) {
        if (value == null) {
            ensureCapacity(buffer.position() + 4);
            buffer.putInt(0);
        } else {
            byte[] bytes = value.getBytes(UTF8Charset.get());
            int len = bytes.length;

            ensureCapacity(buffer.position() + 4 + len);
            buffer.putInt(len);
            buffer.put(bytes);
        }

        return this;
    }

    public ByteStream write(Date value) {
        return write(value == null ? 0L : value.getTime());
    }

    public ByteStream write(Language value) {
        return write(value == null ? null : value.toLanguageTag());
    }

}
