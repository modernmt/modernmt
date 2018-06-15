package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataManager;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 05/08/17.
 */
public class TestData {

    public static final Language EN = new Language("en");
    public static final Language EN_US = new Language("en", "US");
    public static final Language ES = new Language("es");
    public static final Language ES_AR = new Language("es", "AR");
    public static final Language FR = new Language("fr");
    public static final Language FR_CA = new Language("fr", "CA");
    public static final Language IT = new Language("it");
    public static final Language IT_CH = new Language("it", "CH");
    public static final Language NL = new Language("nl");
    public static final Language NL_BE = new Language("nl", "BE");
    public static final Language DE = new Language("de");
    public static final Language DE_LU = new Language("de", "LU");
    public static final Language PT = new Language("pt");
    public static final Language PT_BR = new Language("pt", "BR");

    public static final LanguagePair FR__ES = new LanguagePair(TestData.FR, TestData.ES);
    public static final LanguagePair FR__EN = new LanguagePair(TestData.FR, TestData.EN);
    public static final LanguagePair EN__IT = new LanguagePair(TestData.EN, TestData.IT);
    public static final LanguagePair EN__FR = new LanguagePair(TestData.EN, TestData.FR);
    public static final LanguagePair IT__EN = new LanguagePair(TestData.IT, TestData.EN);
    public static final LanguagePair EN_US__IT = new LanguagePair(TestData.EN_US, TestData.IT);
    public static final LanguagePair IT__EN_US = new LanguagePair(TestData.IT, TestData.EN_US);
    public static final LanguagePair EN_US__IT_CH = new LanguagePair(TestData.EN_US, TestData.IT_CH);
    public static final LanguagePair IT_CH__EN_US = new LanguagePair(TestData.IT_CH, TestData.EN_US);
    public static final LanguagePair EN_US__FR_CA = new LanguagePair(TestData.EN_US, TestData.FR_CA);
    public static final LanguagePair FR_CA__EN_US = new LanguagePair(TestData.FR_CA, TestData.EN_US);

    private static final HashMap<String, String> EXAMPLE_SENTENCES = new HashMap<>();

    static {
        EXAMPLE_SENTENCES.put("en", "Hello world");
        EXAMPLE_SENTENCES.put("it", "Ciao mondo");
        EXAMPLE_SENTENCES.put("fr", "Bonjour monde");
        EXAMPLE_SENTENCES.put("nl", "Hallo wereld");
        EXAMPLE_SENTENCES.put("de", "Hallo Welt");
        EXAMPLE_SENTENCES.put("pt", "Olà mundo");
        EXAMPLE_SENTENCES.put("es", "Hola mundo");
    }

    public static Sentence sentence(String text) {
        String[] tokens = text.split("\\s+");
        Word[] words = new Word[tokens.length];

        for (int i = 0; i < words.length; i++)
            words[i] = new Word(tokens[i], " ");

        return new Sentence(words);
    }

    // Translation units

    public static List<TranslationUnit> tuList(LanguagePair language, int size) {
        return tuList(1L, language, size);
    }

    public static List<TranslationUnit> tuList(long memory, LanguagePair language, int size) {
        return tuList(0, 0L, memory, language, size);
    }

    public static List<TranslationUnit> tuList(int channel, long channelPosition, long memory, LanguagePair language, int size) {
        ArrayList<TranslationUnit> units = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String source = EXAMPLE_SENTENCES.get(language.source.getLanguage());
            String target = EXAMPLE_SENTENCES.get(language.target.getLanguage());

            if (i > 0) {
                source += " " + i;
                target += " " + i;
            }

            units.add(tu(channel, channelPosition++, memory, language, source, target, null));
        }
        return units;
    }

    public static TranslationUnit tu(LanguagePair language, Date timestamp) {
        return tu(1L, language, timestamp);
    }

    public static TranslationUnit tu(long memory, LanguagePair language, Date timestamp) {
        return tu(0, 0L, memory, language, timestamp);
    }

    public static TranslationUnit tu(int channel, long channelPosition, long memory, LanguagePair language, Date timestamp) {
        return tu(channel, channelPosition, memory, language,
                EXAMPLE_SENTENCES.get(language.source.getLanguage()),
                EXAMPLE_SENTENCES.get(language.target.getLanguage()),
                timestamp);
    }

    public static TranslationUnit tu(LanguagePair language, String source, String target, Date timestamp) {
        return tu(1L, language, source, target, timestamp);
    }

    public static TranslationUnit tu(long memory, LanguagePair language, String source, String target, Date timestamp) {
        return tu(0, 0, memory, language, source, target, timestamp);
    }

    public static TranslationUnit tu(int channel, long channelPosition, long memory, LanguagePair language, String source, String target, Date timestamp) {
        return tu(channel, channelPosition, memory, language, source, target, null, null, timestamp);
    }


    public static TranslationUnit tu(int channel, long channelPosition, long memory, LanguagePair language, String source, String target, String previousSource, String previousTarget, Date timestamp) {
        return tu(channel, channelPosition, null, memory, language, source, target, previousSource, previousTarget, timestamp);
    }

    public static TranslationUnit tu(int channel, long channelPosition, UUID owner, long memory, LanguagePair language, String source, String target, String previousSource, String previousTarget, Date timestamp) {
        return new TranslationUnit((short) channel, channelPosition, owner, language, memory,
                source, target, previousSource, previousTarget, timestamp,
                sentence(source), sentence(target), null);
    }

    // Corpus

    public static MultilingualCorpus corpus(String name, List<TranslationUnit> units) {
        final HashMap<LanguagePair, ArrayList<TranslationUnit>> lang2units = new HashMap<>();
        for (TranslationUnit unit : units)
            lang2units.computeIfAbsent(unit.direction, key -> new ArrayList<>()).add(unit);

        return new MultilingualCorpus() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Set<LanguagePair> getLanguages() {
                return lang2units.keySet();
            }

            @Override
            public int getLineCount(LanguagePair language) {
                return lang2units.get(language).size();
            }

            @Override
            public MultilingualLineReader getContentReader() throws IOException {
                return new MultilingualLineReader() {

                    private int index = 0;

                    @Override
                    public StringPair read() throws IOException {
                        if (index < units.size()) {
                            TranslationUnit unit = units.get(index++);

                            return new StringPair(unit.direction, unit.rawSentence, unit.rawTranslation);
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public void close() throws IOException {
                    }

                };
            }

            @Override
            public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Corpus getCorpus(LanguagePair language, boolean source) {
                throw new UnsupportedOperationException();
            }
        };
    }

    // Channels

    public static Map<Short, Long> channels(int channel, long position) {
        Map<Short, Long> result = new HashMap<>(1);
        result.put((short) channel, position);
        return result;
    }

    // Deletion

    public static Deletion deletion(long memory) {
        return new Deletion((short) 1, 0, memory);
    }

    public static Deletion deletion(long channelPosition, long memory) {
        return new Deletion((short) 1, channelPosition, memory);
    }
}
