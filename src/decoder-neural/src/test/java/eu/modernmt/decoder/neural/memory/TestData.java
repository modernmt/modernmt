package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
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

    public static final Locale EN = new Locale("en");
    public static final Locale EN_US = new Locale("en", "US");
    public static final Locale ES = new Locale("es");
    public static final Locale ES_AR = new Locale("es", "AR");
    public static final Locale FR = new Locale("fr");
    public static final Locale FR_CA = new Locale("fr", "CA");
    public static final Locale IT = new Locale("it");
    public static final Locale IT_CH = new Locale("it", "CH");
    public static final Locale NL = new Locale("nl");
    public static final Locale NL_BE = new Locale("nl", "BE");
    public static final Locale DE = new Locale("de");
    public static final Locale DE_LU = new Locale("de", "LU");
    public static final Locale PT = new Locale("pt");
    public static final Locale PT_BR = new Locale("pt", "BR");

    public static final LanguagePair FR__ES = new LanguagePair(TestData.FR, TestData.ES);
    public static final LanguagePair FR__EN = new LanguagePair(TestData.FR, TestData.EN);
    public static final LanguagePair EN__IT = new LanguagePair(TestData.EN, TestData.IT);
    public static final LanguagePair EN__FR = new LanguagePair(TestData.EN, TestData.FR);
    public static final LanguagePair IT__EN = new LanguagePair(TestData.IT, TestData.EN);
    public static final LanguagePair EN_US__IT = new LanguagePair(TestData.EN_US, TestData.IT);
    public static final LanguagePair IT__EN_US = new LanguagePair(TestData.IT, TestData.EN_US);

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

            units.add(tu(channel, channelPosition++, memory, language, source, target));
        }
        return units;
    }

    public static TranslationUnit tu(LanguagePair language) {
        return tu(1L, language);
    }

    public static TranslationUnit tu(long memory, LanguagePair language) {
        return tu(0, 0L, memory, language);
    }

    public static TranslationUnit tu(int channel, long channelPosition, long memory, LanguagePair language) {
        return tu(channel, channelPosition, memory, language,
                EXAMPLE_SENTENCES.get(language.source.getLanguage()),
                EXAMPLE_SENTENCES.get(language.target.getLanguage()));
    }

    public static TranslationUnit tu(LanguagePair language, String source, String target) {
        return tu(1L, language, source, target);
    }

    public static TranslationUnit tu(long memory, LanguagePair language, String source, String target) {
        return tu(0, 0, memory, language, source, target);
    }

    public static TranslationUnit tu(int channel, long channelPosition, long memory, LanguagePair language, String source, String target) {
        TranslationUnit tu = new TranslationUnit((short) channel, channelPosition, language, memory, source, target);
        tu.sourceSentence = sentence(source);
        tu.targetSentence = sentence(target);
        return tu;
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

                            return new StringPair(unit.direction, unit.rawSourceSentence, unit.rawTargetSentence);
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
