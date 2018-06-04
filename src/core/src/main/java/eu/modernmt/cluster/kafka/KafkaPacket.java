package eu.modernmt.cluster.kafka;

import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Date;

/**
 * Created by davide on 06/09/16.
 */
public class KafkaPacket {

    public static final byte TYPE_DELETION = 0x00;
    public static final byte TYPE_ADDITION = 0x01;
    public static final byte TYPE_OVERWRITE = 0x02;

    private short channel = -1;
    private long position = -1;

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
                Charset charset = UTF8Charset.get();

                Language source = Language.fromString(deserializeString(buffer, charset));
                Language target = Language.fromString(deserializeString(buffer, charset));
                direction = new LanguagePair(source, target);

                sentence = deserializeString(buffer, charset);
                translation = deserializeString(buffer, charset);

                long millis = buffer.getLong();
                timestamp = millis == 0L ? null : new Date(millis);

                if (type == TYPE_OVERWRITE) {
                    previousSentence = deserializeString(buffer, charset);
                    previousTranslation = deserializeString(buffer, charset);
                }

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

    public void setChannelInfo(short channel, long position) {
        this.channel = channel;
        this.position = position;
    }

    public byte getType() {
        return type;
    }

    public long getMemory() {
        return memory;
    }

    public LanguagePair getDirection() {
        return direction;
    }

    public String getSentence() {
        return sentence;
    }

    public String getTranslation() {
        return translation;
    }

    public String getPreviousSentence() {
        return previousSentence;
    }

    public String getPreviousTranslation() {
        return previousTranslation;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Deletion asDeletion() {
        if (channel < 0 || position < 0)
            throw new IllegalStateException("Call setChannelInfo() before parsing methods.");

        return new Deletion(channel, position, memory);
    }

    public TranslationUnit asTranslationUnit(LanguagePair direction, Sentence sSentence, Sentence sTranslation, Alignment alignment) {
        if (channel < 0 || position < 0)
            throw new IllegalStateException("Call setChannelInfo() before parsing methods.");

        return new TranslationUnit(channel, position, direction, memory,
                sentence, translation, previousSentence, previousTranslation, timestamp,
                sSentence, sTranslation, alignment);
    }

    /**
     * This method makes this KafkaPacket a series of bytes.
     * This method is typically used to get the bytes that must be sent into a Kafka channel.
     *
     * @return the array of bytes obtained from the original KafkaPacket
     */
    public byte[] toBytes() {
        int size = 1 + 8 + 8;   //type (enum: 1 byte) + memory (long: 8 bytes)

        byte type = this.type;
        long memory = this.memory;

        byte[] directionSource = null;
        byte[] directionTarget = null;
        byte[] sentence = null;
        byte[] translation = null;
        long timestamp = this.timestamp == null ? 0L : this.timestamp.getTime();

        byte[] previousSentence = null;
        byte[] previousTranslation = null;

        switch (type) {
            case TYPE_DELETION:
                break;
            case TYPE_ADDITION:
            case TYPE_OVERWRITE:
                Charset charset = UTF8Charset.get();

                directionSource = this.direction.source.toLanguageTag().getBytes(charset);
                directionTarget = this.direction.target.toLanguageTag().getBytes(charset);
                sentence = this.sentence.getBytes(charset);
                translation = this.translation.getBytes(charset);

                size += 4 + directionSource.length +
                        4 + directionTarget.length +
                        4 + sentence.length +
                        4 + translation.length +
                        8;  // + timestamp (long: 8 bytes)

                if (type == TYPE_OVERWRITE) {
                    previousSentence = this.previousSentence.getBytes(charset);
                    previousTranslation = this.previousTranslation.getBytes(charset);

                    size += 4 + previousSentence.length + 4 + previousTranslation.length;
                }

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

        if (type != TYPE_DELETION)
            buffer.putLong(timestamp);

        serializeString(buffer, previousSentence);
        serializeString(buffer, previousTranslation);

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
