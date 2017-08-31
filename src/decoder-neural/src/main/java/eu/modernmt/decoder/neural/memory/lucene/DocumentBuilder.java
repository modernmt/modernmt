package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import org.apache.lucene.document.*;
import org.apache.lucene.util.BytesRef;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
class DocumentBuilder {

    private static final String CHANNELS_FIELD = "channels";

    public static final String DOMAIN_ID_FIELD = "domain";
    public static final String LANGUAGE_FIELD = "language";
    private static final String CONTENT_PREFIX_FIELD = "content::";

    // TranslationUnit entries

    public static Document build(TranslationUnit unit) {
        return build(unit.direction, unit.domain, unit.sourceSentence, unit.targetSentence);
    }

    public static Document build(LanguagePair direction, long domain, Sentence sentence, Sentence translation) {
        String s = TokensOutputStream.toString(sentence, false, true);
        String t = TokensOutputStream.toString(translation, false, true);
        return build(direction, domain, s, t);
    }

    public static Document build(LanguagePair direction, long domain, String sentence, String translation) {
        Document document = new Document();
        document.add(new LongField(DOMAIN_ID_FIELD, domain, Field.Store.YES));
        document.add(new StringField(LANGUAGE_FIELD, encode(direction), Field.Store.YES));
        document.add(new TextField(getContentFieldName(direction.source), sentence, Field.Store.YES));
        document.add(new TextField(getContentFieldName(direction.target), translation, Field.Store.YES));

        return document;
    }

    public static ScoreEntry parseEntry(LanguagePair direction, Document doc) {
        long domain = Long.parseLong(doc.get(DOMAIN_ID_FIELD));
        String[] sentence = doc.get(getContentFieldName(direction.source)).split(" ");
        String[] translation = doc.get(getContentFieldName(direction.target)).split(" ");

        return new ScoreEntry(domain, sentence, translation);
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

    public static String getContentFieldName(Locale locale) {
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
        document.add(new LongField(DOMAIN_ID_FIELD, 0, Field.Store.YES));
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
