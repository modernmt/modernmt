package eu.modernmt.cluster.kafka;

import eu.modernmt.data.DataMessage;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.DefaultCharset;
import eu.modernmt.lang.LanguagePair;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Locale;

/**
 * Created by davide on 06/09/16.
 */
class KafkaPacket {

    private static final byte TYPE_DELETION = 0x00;
    private static final byte TYPE_ADDITION = 0x01;
    private static final byte TYPE_OVERWRITE = 0x02;

    private final byte type;
    private final long memory;
    private final LanguagePair direction;
    private final String sentence;
    private final String translation;
    private final String previousSentence;
    private final String previousTranslation;
    private final Date timestamp;

    public static KafkaPacket createDeletion(long memory) {
        return new KafkaPacket(TYPE_DELETION, null, memory, null, null, null, null, null);
    }

    public static KafkaPacket createAddition(LanguagePair direction, long memory, String sentence, String translation, Date timestamp) {
        return new KafkaPacket(TYPE_ADDITION, direction, memory, sentence, translation, null, null, timestamp);
    }

    public static KafkaPacket createOverwrite(LanguagePair direction, long memory, String sentence, String translation, String previousSentence, String previousTranslation, Date timestamp) {
        return new KafkaPacket(TYPE_OVERWRITE, direction, memory, sentence, translation, previousSentence, previousTranslation, timestamp);
    }

    /**
     * Parse a KafkaPacket from the bytes read from a Kafka Channel
     *
     * @param data the bytes read from the Kafka Channel
     * @return the parsed data as a KafkaPacket
     */
    public static KafkaPacket fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte type = buffer.get();
        long memory = buffer.getLong();

        LanguagePair direction = null;
        String sentence = null;
        String translation = null;
        String previousSentence = null;
        String previousTranslation = null;
        Date timestamp = null;

        switch (type) {
            case TYPE_DELETION:
                break;
            case TYPE_ADDITION:
            case TYPE_OVERWRITE:
                Charset charset = DefaultCharset.get();

                Locale source = Locale.forLanguageTag(deserializeString(buffer, charset));
                Locale target = Locale.forLanguageTag(deserializeString(buffer, charset));
                direction = new LanguagePair(source, target);

                sentence = deserializeString(buffer, charset);
                translation = deserializeString(buffer, charset);

                if (type == TYPE_OVERWRITE) {
                    previousSentence = deserializeString(buffer, charset);
                    previousTranslation = deserializeString(buffer, charset);
                }

                /*parse the date as millis from epoch as the last field; if 0 then date is null*/
                long millis = Long.parseLong(deserializeString(buffer, charset));
                timestamp = millis == 0L ? null : new Date(millis);

                break;
            default:
                throw new IllegalArgumentException("Invalid packet received, unknown type: " + (int) type);
        }

        return new KafkaPacket(type, direction, memory, sentence, translation, previousSentence, previousTranslation, timestamp);
    }

    private KafkaPacket(byte type, LanguagePair direction, long memory, String sentence, String translation, String previousSentence, String previousTranslation, Date timestamp) {
        this.type = type;
        this.direction = direction;
        this.memory = memory;
        this.sentence = sentence;
        this.translation = translation;
        this.previousSentence = previousSentence;
        this.previousTranslation = previousTranslation;
        this.timestamp = timestamp;
    }

    public DataMessage toDataMessage(short channel, long position) {
        switch (type) {
            case TYPE_DELETION:
                return new Deletion(channel, position, memory);
            case TYPE_ADDITION:
                return new TranslationUnit(channel, position, direction, memory, sentence, translation, null, null, timestamp);
            case TYPE_OVERWRITE:
                return new TranslationUnit(channel, position, direction, memory, sentence, translation, previousSentence, previousTranslation, timestamp);
            default:
                throw new IllegalArgumentException("Invalid packet received, unknown type: " + (int) type);
        }
    }

    /**
     * This method makes this KafkaPacket a series of bytes.
     * This method is typically used to get the bytes that must be sent into a Kafka channel.
     *
     * @return the array of bytes obtained from the original KafkaPacket
     */
    public byte[] toBytes() {
        int size = 9;

        byte type = this.type;
        long memory = this.memory;

        byte[] directionSource = null;
        byte[] directionTarget = null;
        byte[] sentence = null;
        byte[] translation = null;

        byte[] previousSentence = null;
        byte[] previousTranslation = null;
        byte[] timestamp = null;

        switch (type) {
            case TYPE_DELETION:
                break;
            case TYPE_ADDITION:
            case TYPE_OVERWRITE:
                Charset charset = DefaultCharset.get();

                directionSource = this.direction.source.toLanguageTag().getBytes(charset);
                directionTarget = this.direction.target.toLanguageTag().getBytes(charset);
                sentence = this.sentence.getBytes(charset);
                translation = this.translation.getBytes(charset);

                size += 4 + directionSource.length + 4 + directionTarget.length +
                        4 + sentence.length + 4 + translation.length;

                if (type == TYPE_OVERWRITE) {
                    previousSentence = this.previousSentence.getBytes(charset);
                    previousTranslation = this.previousTranslation.getBytes(charset);

                    size += 4 + previousSentence.length + 4 + previousTranslation.length;
                }

                long millis = this.timestamp == null ? 0L : this.timestamp.getTime();
                timestamp = Long.toString(millis).getBytes();
                size += 4 + timestamp.length;

                break;
            default:
                throw new IllegalArgumentException("Invalid packet received, unknown type: " + (int) type);
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);

        buffer.put(type);
        buffer.putLong(memory);

        serializeString(buffer, directionSource);
        serializeString(buffer, directionTarget);
        serializeString(buffer, sentence);
        serializeString(buffer, translation);

        serializeString(buffer, previousSentence);
        serializeString(buffer, previousTranslation);

        serializeString(buffer, timestamp);

        return buffer.array();
    }

    @Override
    public String toString() {
        return "<" + memory + "::" + direction + ":\"" + sentence + "\",\"" + translation + "\">";
    }

    private static String deserializeString(ByteBuffer buffer, Charset charset) {
        int length = buffer.getInt();
        String string = new String(buffer.array(), buffer.position(), length, charset);
        buffer.position(buffer.position() + length);

        return string;
    }

    private static void serializeString(ByteBuffer buffer, byte[] string) {
        if (string != null) {
            buffer.putInt(string.length);
            buffer.put(string);
        }
    }
}
