package eu.modernmt.cluster.kafka;

import eu.modernmt.data.DeletionMessage;
import eu.modernmt.data.TranslationUnitMessage;
import eu.modernmt.io.ByteStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.TranslationUnit;

import java.util.Date;
import java.util.UUID;

/**
 * Created by davide on 06/09/16.
 */
public class KafkaPacket {

    public static final byte TYPE_DELETION = 0x00;
    public static final byte TYPE_ADDITION = 0x01;
    public static final byte TYPE_OVERWRITE_BY_VALUE = 0x02;
    public static final byte TYPE_OVERWRITE_BY_TUID = 0x03;

    private short channel = -1;
    private long position = -1;

    private final byte type;
    private final UUID owner;
    private final long memory;
    private final LanguageDirection direction;
    private final String tuid;
    private final String sentence;
    private final String translation;
    private final Date timestamp;
    private final String previousSentence;
    private final String previousTranslation;

    public static KafkaPacket createDeletion(long memory) {
        return new KafkaPacket(TYPE_DELETION, null, memory, null, null, null, null, null, null, null);
    }

    public static KafkaPacket createAddition(UUID owner, long memory, TranslationUnit tu) {
        return new KafkaPacket(TYPE_ADDITION, owner, memory, tu.language, tu.tuid, tu.source, tu.target, tu.timestamp, null, null);
    }

    public static KafkaPacket createOverwrite(UUID owner, long memory, TranslationUnit tu) {
        return new KafkaPacket(TYPE_OVERWRITE_BY_TUID, owner, memory, tu.language, tu.tuid, tu.source, tu.target, tu.timestamp, null, null);
    }

    public static KafkaPacket createOverwrite(UUID owner, long memory, TranslationUnit tu, String previousSentence, String previousTranslation) {
        return new KafkaPacket(TYPE_OVERWRITE_BY_VALUE, owner, memory, tu.language, tu.tuid, tu.source, tu.target, tu.timestamp, previousSentence, previousTranslation);
    }

    private KafkaPacket(byte type, UUID owner, long memory,
                        LanguageDirection direction, String tuid, String sentence, String translation, Date timestamp,
                        String previousSentence, String previousTranslation) {
        this.type = type;
        this.owner = owner;
        this.memory = memory;

        this.direction = direction;
        this.tuid = tuid;
        this.sentence = sentence;
        this.translation = translation;
        this.timestamp = timestamp;

        this.previousSentence = previousSentence;
        this.previousTranslation = previousTranslation;
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

    public LanguageDirection getDirection() {
        return direction;
    }

    public String getSentence() {
        return sentence;
    }

    public String getTranslation() {
        return translation;
    }

    public DeletionMessage asDeletion() {
        if (channel < 0 || position < 0)
            throw new IllegalStateException("Call setChannelInfo() before parsing methods.");

        return new DeletionMessage(channel, position, memory);
    }

    public TranslationUnitMessage asTranslationUnit(LanguageDirection language) {
        return this.asTranslationUnit(language, null, null, null);
    }

    public TranslationUnitMessage asTranslationUnit(LanguageDirection language, Sentence sSentence, Sentence sTranslation, Alignment alignment) {
        if (channel < 0 || position < 0)
            throw new IllegalStateException("Call setChannelInfo() before parsing methods.");

        boolean update = (type == TYPE_OVERWRITE_BY_VALUE) || (type == TYPE_OVERWRITE_BY_TUID);
        TranslationUnit tu = new TranslationUnit(this.tuid, this.direction, this.sentence, this.translation, this.timestamp);

        return new TranslationUnitMessage(channel, position, memory, owner, tu,
                update, previousSentence, previousTranslation,
                language, sSentence, sTranslation, alignment);
    }

    public static KafkaPacket fromBytes(byte[] data) {
        ByteStream buffer = new ByteStream(data);
        byte type = buffer.readByte();
        long memory = buffer.readLong();

        if (type == TYPE_DELETION)
            return createDeletion(memory);

        final UUID owner = buffer.readUUID();
        final LanguageDirection language = new LanguageDirection(buffer.readLanguage(), buffer.readLanguage());
        final String sentence = buffer.readString();
        final String translation = buffer.readString();
        final Date timestamp = buffer.readDate();

        String previousSentence = null;
        String previousTranslation = null;

        if (type == TYPE_OVERWRITE_BY_VALUE) {
            previousSentence = buffer.readString();
            previousTranslation = buffer.readString();
        }

        String tuid = buffer.readString();

        return new KafkaPacket(type, owner, memory, language, tuid, sentence, translation, timestamp, previousSentence, previousTranslation);
    }

    public byte[] toBytes() {
        ByteStream buffer = new ByteStream();
        buffer.write(type)
                .write(memory);

        if (type == TYPE_DELETION)
            return buffer.toArray();

        buffer.write(owner)
                .write(direction.source)
                .write(direction.target)
                .write(sentence)
                .write(translation)
                .write(timestamp);

        if (type == TYPE_OVERWRITE_BY_VALUE) {
            buffer.write(previousSentence)
                    .write(previousTranslation);
        }

        buffer.write(tuid);

        return buffer.toArray();
    }

    @Override
    public String toString() {
        return "KafkaPacket{" +
                "type=" + type +
                ", owner=" + owner +
                ", memory=" + memory +
                ", direction=" + direction +
                ", tuid='" + tuid + '\'' +
                ", sentence='" + sentence + '\'' +
                ", translation='" + translation + '\'' +
                ", timestamp=" + timestamp +
                ", previousSentence='" + previousSentence + '\'' +
                ", previousTranslation='" + previousTranslation + '\'' +
                '}';
    }

}
