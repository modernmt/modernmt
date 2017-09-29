package eu.modernmt.cluster.kafka;

import eu.modernmt.data.DataMessage;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.lang.LanguagePair;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Created by davide on 06/09/16.
 */
class KafkaPacket {

    private static final byte TYPE_UPDATE = 0x00;
    private static final byte TYPE_DELETION = 0x01;

    private final byte type;
    private final long memory;
    private final LanguagePair direction;
    private final String sourceSentence;
    private final String targetSentence;

    public static KafkaPacket createUpdate(LanguagePair direction, long memory, String sourceSentence, String targetSentence) {
        return new KafkaPacket(TYPE_UPDATE, direction, memory, sourceSentence, targetSentence);
    }

    public static KafkaPacket createDeletion(long memory) {
        return new KafkaPacket(TYPE_DELETION, null, memory, null, null);
    }

    public static KafkaPacket fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte type = buffer.get();
        long memory = buffer.getLong();

        LanguagePair direction = null;
        String sourceSentence = null;
        String targetSentence = null;


        switch (type) {
            case TYPE_UPDATE:
                Charset charset = DefaultCharset.get();

                Locale source = Locale.forLanguageTag(deserializeString(buffer, charset));
                Locale target = Locale.forLanguageTag(deserializeString(buffer, charset));

                direction = new LanguagePair(source, target);
                sourceSentence = deserializeString(buffer, charset);
                targetSentence = deserializeString(buffer, charset);

                break;
            case TYPE_DELETION:
                break;
            default:
                throw new IllegalArgumentException("Invalid packet received, unknown type: " + (int) type);
        }

        return new KafkaPacket(type, direction, memory, sourceSentence, targetSentence);
    }

    private static String deserializeString(ByteBuffer buffer, Charset charset) {
        int length = buffer.getInt();
        String string = new String(buffer.array(), buffer.position(), length, charset);
        buffer.position(buffer.position() + length);

        return string;
    }

    private KafkaPacket(byte type, LanguagePair direction, long memory, String sourceSentence, String targetSentence) {
        this.type = type;
        this.direction = direction;
        this.memory = memory;
        this.sourceSentence = sourceSentence;
        this.targetSentence = targetSentence;
    }

    public DataMessage toDataMessage(short channel, long position) {
        switch (type) {
            case TYPE_UPDATE:
                return new TranslationUnit(channel, position, direction, memory, sourceSentence, targetSentence);
            case TYPE_DELETION:
                return new Deletion(channel, position, memory);
            default:
                throw new IllegalArgumentException("Invalid packet received, unknown type: " + (int) type);
        }
    }

    public byte[] toBytes() {
        ByteBuffer buffer;

        switch (type) {
            case TYPE_UPDATE:
                Charset charset = DefaultCharset.get();

                byte[] sourceSentenceBytes = sourceSentence.getBytes(charset);
                byte[] targetSentenceBytes = targetSentence.getBytes(charset);
                byte[] sourceBytes = direction.source.toLanguageTag().getBytes(charset);
                byte[] targetBytes = direction.target.toLanguageTag().getBytes(charset);

                buffer = ByteBuffer.allocate(25 +
                        sourceSentenceBytes.length + targetSentenceBytes.length +
                        sourceBytes.length + targetBytes.length);

                buffer.put(type);
                buffer.putLong(memory);
                buffer.putInt(sourceBytes.length);
                buffer.put(sourceBytes);
                buffer.putInt(targetBytes.length);
                buffer.put(targetBytes);
                buffer.putInt(sourceSentenceBytes.length);
                buffer.put(sourceSentenceBytes);
                buffer.putInt(targetSentenceBytes.length);
                buffer.put(targetSentenceBytes);

                break;
            case TYPE_DELETION:
                buffer = ByteBuffer.allocate(9);
                buffer.put(type);
                buffer.putLong(memory);

                break;
            default:
                throw new IllegalArgumentException("Invalid packet received, unknown type: " + (int) type);
        }

        return buffer.array();
    }

    @Override
    public String toString() {
        return "<" + memory + "::" + direction + ":\"" + sourceSentence + "\",\"" + targetSentence + "\">";
    }

}
