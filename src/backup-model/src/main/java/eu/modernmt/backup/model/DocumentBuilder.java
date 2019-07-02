package eu.modernmt.backup.model;

import eu.modernmt.data.HashGenerator;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DocumentBuilder {

    private static final String CHANNELS_FIELD = "channels";

    private static final String HASH_FIELD = "hash";
    private static final String MEMORY_FIELD = "memory";
    private static final String OWNER_FIELD = "owner";
    private static final String SRC_LANGUAGE_FIELD = "src_language";
    private static final String TGT_LANGUAGE_FIELD = "tgt_language";
    private static final String SENTENCE_FIELD = "sentence";
    private static final String TRANSLATION_FIELD = "translation";
    private static final String TIMESTAMP_FIELD = "timestamp";

    // Factory methods

    public static Document newInstance(TranslationUnit unit) {
        String hash = HashGenerator.hash(unit.rawLanguage, unit.rawSentence, unit.rawTranslation);

        Document document = new Document();
        document.add(new HashField(HASH_FIELD, hash, Field.Store.NO));
        document.add(new LongField(MEMORY_FIELD, unit.memory, Field.Store.YES));
        if (unit.owner != null)
            document.add(new StoredField(OWNER_FIELD, unit.owner.toString()));
        document.add(new StoredField(SRC_LANGUAGE_FIELD, unit.rawLanguage.source.toLanguageTag()));
        document.add(new StoredField(TGT_LANGUAGE_FIELD, unit.rawLanguage.target.toLanguageTag()));
        document.add(new StoredField(SENTENCE_FIELD, unit.rawSentence));
        document.add(new StoredField(TRANSLATION_FIELD, unit.rawTranslation));
        if (unit.timestamp != null)
            document.add(new StoredField(TIMESTAMP_FIELD, Long.toString(unit.timestamp.getTime())));

        return document;
    }

    public static Document newChannelsInstance(HashMap<Short, Long> channels) {
        ByteBuffer buffer = ByteBuffer.allocate(10 * channels.size());
        for (Map.Entry<Short, Long> entry : channels.entrySet()) {
            buffer.putShort(entry.getKey());
            buffer.putLong(entry.getValue());
        }

        Document document = new Document();
        document.add(new LongField(MEMORY_FIELD, 0, Field.Store.YES));
        document.add(new StoredField(CHANNELS_FIELD, buffer.array()));

        return document;
    }

    // Getters

    public static long getMemory(Document document) {
        return Long.parseLong(document.get(MEMORY_FIELD));
    }

    // Parsing

    public static Map<Short, Long> asChannels(Document document) {
        HashMap<Short, Long> result = new HashMap<>();

        BytesRef value = document.getBinaryValue(CHANNELS_FIELD);
        ByteBuffer buffer = ByteBuffer.wrap(value.bytes);

        while (buffer.hasRemaining()) {
            short channel = buffer.getShort();
            long position = buffer.getLong();
            result.put(channel, position);
        }

        return result;
    }

    public static BackupEntry asEntry(Document document) {
        UUID owner = getUUID(document, OWNER_FIELD);
        Language source = getLanguage(document, SRC_LANGUAGE_FIELD);
        Language target = getLanguage(document, TGT_LANGUAGE_FIELD);
        LanguageDirection language = new LanguageDirection(source, target);
        long memory = getLong(document, MEMORY_FIELD);
        String sentence = document.get(SENTENCE_FIELD);
        String translation = document.get(TRANSLATION_FIELD);
        Date timestamp = getDate(document, TIMESTAMP_FIELD);

        return new BackupEntry(owner, language, memory, sentence, translation, timestamp);
    }

    private static UUID getUUID(Document document, String field) {
        String value = document.get(field);
        return value == null ? null : UUID.fromString(value);
    }

    private static Language getLanguage(Document document, String field) {
        return Language.fromString(document.get(field));
    }

    private static long getLong(Document document, String field) {
        return Long.parseLong(document.get(field));
    }

    private static Date getDate(Document document, String field) {
        String value = document.get(field);
        return value == null ? null : new Date(Long.parseLong(value));
    }

    // Term constructors

    public static Term makeChannelsTerm() {
        return makeMemoryTerm(0L);
    }


    public static Term makeMemoryTerm(long memory) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(memory, 0, builder);

        return new Term(MEMORY_FIELD, builder.toBytesRef());
    }


    public static Term makeHashTerm(String hash) {
        return new Term(HASH_FIELD, hash);
    }

}
