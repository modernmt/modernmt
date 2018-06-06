package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.ScoreEntry;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public class DocumentBuilder {

    // Factory methods

    public static Document newInstance(TranslationUnit unit) {
        String sentence = TokensOutputStream.serialize(unit.sentence, false, true);
        String translation = TokensOutputStream.serialize(unit.translation, false, true);
        String hash = HashGenerator.hash(unit.rawSentence, unit.rawTranslation);

        return newInstance(unit.direction, unit.owner, unit.memory, sentence, translation, hash);
    }

    public static Document newInstance(LanguagePair direction, long owner, long memory, String sentence, String translation) {
        return newInstance(direction, owner, memory, sentence, translation, null);
    }

    public static Document newInstance(LanguagePair direction, long owner, long memory, String sentence, String translation, String hash) {
        Document document = new Document();
        document.add(new LongField(MEMORY_FIELD, memory, Field.Store.YES));
        document.add(new LongField(OWNER_FIELD, owner, Field.Store.NO));

        if (hash != null)
            document.add(new HashField(HASH_FIELD, hash, Field.Store.NO));

        document.add(new StringField(makeLanguageFieldName(direction.source), direction.source.toLanguageTag(), Field.Store.YES));
        document.add(new StringField(makeLanguageFieldName(direction.target), direction.target.toLanguageTag(), Field.Store.YES));
        document.add(new TextField(makeContentFieldName(direction), sentence, Field.Store.YES));
        document.add(new TextField(makeContentFieldName(direction.reversed()), translation, Field.Store.YES));

        return document;
    }

    public static Document newChannelsInstance(Map<Short, Long> channels) {
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

    private static final String CHANNELS_FIELD = "channels";
    private static final String MEMORY_FIELD = "memory";
    private static final String OWNER_FIELD = "owner";
    private static final String HASH_FIELD = "hash";
    private static final String LANGUAGE_PREFIX_FIELD = "lang_";
    private static final String CONTENT_PREFIX_FIELD = "content_";

    // Getters

    public static long getMemory(Document self) {
        return Long.parseLong(self.get(MEMORY_FIELD));
    }

    // Parsing

    public static ScoreEntry asEntry(Document self) {
        Language source = null;
        Language target = null;

        for (IndexableField field : self.getFields()) {
            String name = field.name();

            if (name.startsWith(LANGUAGE_PREFIX_FIELD)) {
                Language l = Language.fromString(name.substring(LANGUAGE_PREFIX_FIELD.length()));

                if (source == null) {
                    source = l;
                } else {
                    target = l;
                    break;
                }
            }
        }

        if (source == null || target == null)
            throw new IllegalArgumentException("Invalid document: missing language info.");

        if (source.compareTo(target) < 0)
            return asEntry(self, new LanguagePair(source, target));
        else
            return asEntry(self, new LanguagePair(target, source));
    }

    public static ScoreEntry asEntry(Document self, LanguagePair direction) {
        long memory = Long.parseLong(self.get(MEMORY_FIELD));
        String[] sentence = self.get(makeContentFieldName(direction)).split(" ");
        String[] translation = self.get(makeContentFieldName(direction.reversed())).split(" ");

        String _source = self.get(makeLanguageFieldName(direction.source));
        String _target = self.get(makeLanguageFieldName(direction.target));

        boolean differ = false;
        Language source = direction.source;
        Language target = direction.target;

        if (!_source.equals(direction.source.toLanguageTag())) {
            source = Language.fromString(_source);
            differ = true;
        }

        if (!_target.equals(direction.target.toLanguageTag())) {
            target = Language.fromString(_target);
            differ = true;
        }

        if (differ)
            direction = new LanguagePair(source, target);

        return new ScoreEntry(memory, direction, sentence, translation);
    }

    public static Map<Short, Long> asChannels(Document self) {
        HashMap<Short, Long> result = new HashMap<>();

        BytesRef value = self.getBinaryValue(CHANNELS_FIELD);
        ByteBuffer buffer = ByteBuffer.wrap(value.bytes);

        while (buffer.hasRemaining()) {
            short channel = buffer.getShort();
            long position = buffer.getLong();
            result.put(channel, position);
        }

        return result;
    }

    // Term constructors

    public static Term makeHashTerm(String h) {
        return new Term(HASH_FIELD, h);
    }

    public static Term makeMemoryTerm(long memory) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(memory, 0, builder);

        return new Term(MEMORY_FIELD, builder.toBytesRef());
    }

    public static Term makeOwnerTerm(long owner) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(owner, 0, builder);

        return new Term(OWNER_FIELD, builder.toBytesRef());
    }

    public static Term makeChannelsTerm() {
        return makeMemoryTerm(0L);
    }

    public static Term makeLanguageTerm(Language language) {
        return new Term(makeLanguageFieldName(language), language.toLanguageTag());
    }

    // Fields builders

    public static boolean isHashField(String field) {
        return HASH_FIELD.equals(field);
    }

    public static String makeLanguageFieldName(Language language) {
        return LANGUAGE_PREFIX_FIELD + language.getLanguage();
    }

    public static String makeContentFieldName(LanguagePair direction) {
        return CONTENT_PREFIX_FIELD + direction.source.getLanguage() + '_' + direction.target.getLanguage();
    }

}
