package eu.modernmt.cluster.kafka;

import eu.modernmt.data.DataMessage;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.DefaultCharset;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by davide on 06/09/16.
 */
class KafkaElement {

    private final byte flags;
    private final int domain;
    private final String sourceSentence;
    private final String targetSentence;

    public static KafkaElement createUpdate(int domain, String sourceSentence, String targetSentence) {
        return new KafkaElement((byte) 0, domain, sourceSentence, targetSentence);
    }

    public static KafkaElement createDeletion(int domain) {
        return new KafkaElement((byte) 1, domain, null, null);
    }

    public static KafkaElement fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte flags = buffer.get(); // flags: reserved for future use

        int domain = buffer.getInt();

        if (flags == 0) { // update
            Charset charset = DefaultCharset.get();

            int sourceSentenceLength = buffer.getInt();
            String sourceSentence = new String(data, 9, sourceSentenceLength, charset);
            buffer.position(9 + sourceSentenceLength);

            int targetSentenceLength = buffer.getInt();
            String targetSentence = new String(data, 13 + sourceSentenceLength, targetSentenceLength, charset);

            return new KafkaElement(flags, domain, sourceSentence, targetSentence);
        } else { // deletion
            return new KafkaElement(flags, domain, null, null);
        }
    }

    private KafkaElement(byte flags, int domain, String sourceSentence, String targetSentence) {
        this.flags = flags;
        this.domain = domain;
        this.sourceSentence = sourceSentence;
        this.targetSentence = targetSentence;
    }

    public DataMessage toDataMessage(short channel, long position) {
        if (flags == 0)
            return new TranslationUnit(channel, position, domain, sourceSentence, targetSentence);
        else
            return new Deletion(channel, position, domain);
    }

    public byte[] toBytes() {
        ByteBuffer buffer;

        if (flags == 0) { // update
            Charset charset = DefaultCharset.get();

            byte[] sourceSentenceBytes = sourceSentence.getBytes(charset);
            byte[] targetSentenceBytes = targetSentence.getBytes(charset);

            buffer = ByteBuffer.allocate(13 + sourceSentenceBytes.length + targetSentenceBytes.length);
            buffer.put(flags);
            buffer.putInt(domain);
            buffer.putInt(sourceSentenceBytes.length);
            buffer.put(sourceSentenceBytes);
            buffer.putInt(targetSentenceBytes.length);
            buffer.put(targetSentenceBytes);
        } else { // deletion
            buffer = ByteBuffer.allocate(5);
            buffer.put(flags);
            buffer.putInt(domain);
        }

        return buffer.array();
    }

    @Override
    public String toString() {
        return "<" + domain + ":\"" + sourceSentence + "\",\"" + targetSentence + "\">";
    }
}
