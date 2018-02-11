package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public class DocumentBuilder {

    private static final String CHANNELS_FIELD = "channels";

    public static final String MEMORY_ID_FIELD = "memory";
    public static final String HASH_FIELD = "hash";
    public static final String LANGUAGE_FIELD = "language";
    private static final String CONTENT_PREFIX_FIELD = "content::";

    // TranslationUnit entries

    public static Document build(LanguagePair direction, TranslationUnit unit) {
        String sentence = TokensOutputStream.serialize(unit.sentence, false, true);
        String translation = TokensOutputStream.serialize(unit.translation, false, true);
        String hash = HashGenerator.hash(direction, unit.rawSentence, unit.rawTranslation);

        return build(direction, unit.memory, sentence, translation, hash);
    }

    public static Document build(LanguagePair direction, long memory, String sentence, String translation) {
        return build(direction, memory, sentence, translation, null);
    }

    public static Document build(LanguagePair direction, long memory, String sentence, String translation, String hash) {
        Document document = new Document();
        document.add(new LongField(MEMORY_ID_FIELD, memory, Field.Store.YES));
        document.add(new StringField(LANGUAGE_FIELD, encode(direction), Field.Store.YES));
        document.add(new TextField(getContentFieldName(direction.source), sentence, Field.Store.YES));
        document.add(new TextField(getContentFieldName(direction.target), translation, Field.Store.YES));

        if (hash != null)
            document.add(new HashField(HASH_FIELD, hash, Field.Store.NO));

        return document;
    }

    public static ScoreEntry parseEntry(LanguagePair direction, Document doc) {
        long memory = Long.parseLong(doc.get(MEMORY_ID_FIELD));
        String[] sentence = doc.get(getContentFieldName(direction.source)).split(" ");
        String[] translation = doc.get(getContentFieldName(direction.target)).split(" ");

        return new ScoreEntry(memory, sentence, translation);
    }

    public static String encode(LanguagePair direction) {
        String l1 = direction.source.toLanguageTag();
        String l2 = direction.target.toLanguageTag();

        if (l1.compareTo(l2) > 0) {
            String tmp = l1;
            l1 = l2;
            l2 = tmp;
        }

        return l1 + "__" + l2;
    }

    public static String getContentFieldName(Language locale) {
        return CONTENT_PREFIX_FIELD + locale.toLanguageTag();
    }

    // Channels data entry

    public static Document build(Map<Short, Long> channels) {
        ByteBuffer buffer = ByteBuffer.allocate(10 * channels.size());
        for (Map.Entry<Short, Long> entry : channels.entrySet()) {
            buffer.putShort(entry.getKey());
            buffer.putLong(entry.getValue());
        }

        Document document = new Document();
        document.add(new LongField(MEMORY_ID_FIELD, 0, Field.Store.YES));
        document.add(new StoredField(CHANNELS_FIELD, buffer.array()));

        return document;
    }

    public static Map<Short, Long> parseChannels(Document document) {
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

}
