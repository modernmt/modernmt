package eu.modernmt.util;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by davide on 27/11/15.
 */
public class UUIDUtils {

    public static final int UUID_LEN_BYTES = 16;

    public static UUID parse(byte[] bytes) {
        return parse(bytes, 0);
    }

    public static UUID parse(byte[] bytes, int offset) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, UUID_LEN_BYTES);
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();

        return new UUID(mostSigBits, leastSigBits);
    }

    public static byte[] getBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(UUID_LEN_BYTES);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        return buffer.array();
    }
}
