package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.data.HashGenerator;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.memory.TranslationMemory;
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
public class DefaultDocumentBuilder implements DocumentBuilder {

    private static final String CHANNELS_FIELD = "channels";
    private static final String MEMORY_FIELD = "memory";
    private static final String HASH_FIELD = "hash";
    private static final String LANGUAGE_PREFIX_FIELD = "lang_";
    private static final String CONTENT_PREFIX_FIELD = "content_";

    private static final String SOURCE_LANGUAGE_FIELD = "src_lang";
    private static final String TARGET_LANGUAGE_FIELD = "tgt_lang";
    private static final String SENTENCE_FIELD = "sentence";
    private static final String TRANSLATION_FIELD = "translation";

    // Factory methods

    @Override
    public Document create(TranslationUnit unit) {
        String hash = HashGenerator.hash(unit.rawLanguage, unit.rawSentence, unit.rawTranslation);
        return create(unit, hash);
    }

    public Document create(TranslationUnit unit, String hash) {
        LanguageDirection language = unit.language;
        String sentence = TokensOutputStream.serialize(unit.sentence, false, true);
        String translation = TokensOutputStream.serialize(unit.translation, false, true);

        Document document = new Document();
        document.add(new LongField(MEMORY_FIELD, unit.memory, Field.Store.YES));
        document.add(new HashField(HASH_FIELD, hash, Field.Store.NO));

        document.add(new StringField(makeLanguageFieldName(language.source), language.source.toLanguageTag(), Field.Store.YES));
        document.add(new StringField(makeLanguageFieldName(language.target), language.target.toLanguageTag(), Field.Store.YES));
        document.add(new TextField(makeContentFieldName(language), sentence, Field.Store.YES));
        document.add(new TextField(makeContentFieldName(language.reversed()), translation, Field.Store.YES));

        document.add(new StoredField(SOURCE_LANGUAGE_FIELD, unit.rawLanguage.source.toLanguageTag()));
        document.add(new StoredField(TARGET_LANGUAGE_FIELD, unit.rawLanguage.target.toLanguageTag()));
        document.add(new StoredField(SENTENCE_FIELD, unit.rawSentence.getBytes(UTF8Charset.get())));
        document.add(new StoredField(TRANSLATION_FIELD, unit.rawTranslation.getBytes(UTF8Charset.get())));

        return document;
    }

    @Override
    public Document create(Map<Short, Long> channels) {
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

    @Override
    public long getMemory(Document self) {
        return Long.parseLong(self.get(MEMORY_FIELD));
    }

    @Override
    public String getSourceLanguage(String fieldName) {
        if (!fieldName.startsWith(CONTENT_PREFIX_FIELD))
            throw new IllegalArgumentException("Unexpected field name: " + fieldName);

        int lastUnderscore = fieldName.lastIndexOf('_');
        return fieldName.substring(CONTENT_PREFIX_FIELD.length(), lastUnderscore);
    }

    @Override
    public String getTargetLanguage(String fieldName) {
        if (!fieldName.startsWith(CONTENT_PREFIX_FIELD))
            throw new IllegalArgumentException("Unexpected field name: " + fieldName);

        int lastUnderscore = fieldName.lastIndexOf('_');

        return fieldName.substring(lastUnderscore + 1);
    }

    // Parsing

    @Override
    public ScoreEntry asScoreEntry(Document self) {
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

        LanguageDirection language;
        if (source.toLanguageTag().compareTo(target.toLanguageTag()) < 0)
            language = new LanguageDirection(source, target);
        else
            language = new LanguageDirection(target, source);

        return asScoreEntry(self, language);
    }

    @Override
    public ScoreEntry asScoreEntry(Document self, LanguageDirection direction) {
        long memory = Long.parseLong(self.get(MEMORY_FIELD));
        String[] sentenceTokens = TokensOutputStream.deserialize(self.get(makeContentFieldName(direction)));
        String[] translationTokens = TokensOutputStream.deserialize(self.get(makeContentFieldName(direction.reversed())));

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
            direction = new LanguageDirection(source, target);

        return new ScoreEntry(memory, direction, sentenceTokens, translationTokens);
    }

    @Override
    public TranslationMemory.Entry asEntry(Document self) {
        long memory = Long.parseLong(self.get(MEMORY_FIELD));
        Language source = Language.fromString(self.get(SOURCE_LANGUAGE_FIELD));
        Language target = Language.fromString(self.get(TARGET_LANGUAGE_FIELD));
        String sentence = self.getBinaryValue(SENTENCE_FIELD).utf8ToString();
        String translation = self.getBinaryValue(TRANSLATION_FIELD).utf8ToString();

        return new TranslationMemory.Entry(memory, new LanguageDirection(source, target), sentence, translation);
    }

    @Override
    public Map<Short, Long> asChannels(Document self) {
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

    @Override
    public Term makeHashTerm(String h) {
        return new Term(HASH_FIELD, h);
    }

    @Override
    public Term makeMemoryTerm(long memory) {
        return makeLongTerm(memory, MEMORY_FIELD);
    }

    @Override
    public Term makeChannelsTerm() {
        return makeMemoryTerm(0L);
    }

    @Override
    public Term makeLanguageTerm(Language language) {
        return new Term(makeLanguageFieldName(language), language.toLanguageTag());
    }

    // Fields builders

    @Override
    public boolean isHashField(String field) {
        return HASH_FIELD.equals(field);
    }

    @Override
    public String makeLanguageFieldName(Language language) {
        return LANGUAGE_PREFIX_FIELD + language.getLanguage();
    }

    @Override
    public String makeContentFieldName(LanguageDirection direction) {
        return CONTENT_PREFIX_FIELD + direction.source.getLanguage() + '_' + direction.target.getLanguage();
    }

    // Utils

    private static Term makeLongTerm(long value, String field) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(value, 0, builder);

        return new Term(field, builder.toBytesRef());
    }

}
