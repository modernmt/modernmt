package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DeletionMessage;
import eu.modernmt.data.TranslationUnitMessage;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.model.corpus.*;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 05/08/17.
 */
public class TestData {

    public static final Language EN = Language.fromString("en");
    public static final Language EN_US = Language.fromString("en-US");
    public static final Language ES = Language.fromString("es");
    public static final Language FR = Language.fromString("fr");
    public static final Language FR_CA = Language.fromString("fr-CA");
    public static final Language IT = Language.fromString("it");
    public static final Language IT_CH = Language.fromString("it-CH");

    public static final LanguageDirection FR__ES = new LanguageDirection(TestData.FR, TestData.ES);
    public static final LanguageDirection FR__EN = new LanguageDirection(TestData.FR, TestData.EN);
    public static final LanguageDirection EN__IT = new LanguageDirection(TestData.EN, TestData.IT);
    public static final LanguageDirection EN__FR = new LanguageDirection(TestData.EN, TestData.FR);
    public static final LanguageDirection IT__EN = new LanguageDirection(TestData.IT, TestData.EN);
    public static final LanguageDirection EN_US__IT_CH = new LanguageDirection(TestData.EN_US, TestData.IT_CH);
    public static final LanguageDirection IT_CH__EN_US = new LanguageDirection(TestData.IT_CH, TestData.EN_US);
    public static final LanguageDirection EN_US__FR_CA = new LanguageDirection(TestData.EN_US, TestData.FR_CA);
    public static final LanguageDirection FR_CA__EN_US = new LanguageDirection(TestData.FR_CA, TestData.EN_US);

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
            words[i] = new Word(tokens[i], " ", " ");

        return new Sentence(words);
    }

    // Translation units

    public static List<TranslationUnitMessage> tuList(LanguageDirection language, int size) {
        return tuList(1L, language, size);
    }

    public static List<TranslationUnitMessage> tuList(long memory, LanguageDirection language, int size) {
        return tuList(0, 0L, memory, language, size);
    }

    public static List<TranslationUnitMessage> tuList(int channel, long channelPosition, long memory, LanguageDirection language, int size) {
        ArrayList<TranslationUnitMessage> units = new ArrayList<>(size);
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

    public static TranslationUnitMessage tu(LanguageDirection language, Date timestamp) {
        return tu(1L, language, timestamp);
    }

    public static TranslationUnitMessage tu(long memory, LanguageDirection language, Date timestamp) {
        return tu(0, 0L, memory, language, timestamp);
    }

    public static TranslationUnitMessage tu(int channel, long channelPosition, long memory, LanguageDirection language, Date timestamp) {
        return tu(channel, channelPosition, memory, language,
                EXAMPLE_SENTENCES.get(language.source.getLanguage()),
                EXAMPLE_SENTENCES.get(language.target.getLanguage()),
                timestamp);
    }

    public static TranslationUnitMessage tu(LanguageDirection language, String source, String target, Date timestamp) {
        return tu(1L, language, source, target, timestamp);
    }

    public static TranslationUnitMessage tu(long memory, LanguageDirection language, String source, String target, Date timestamp) {
        return tu(0, 0, memory, language, source, target, timestamp);
    }

    public static TranslationUnitMessage tu(int channel, long channelPosition, long memory, LanguageDirection language, String source, String target, Date timestamp) {
        return tu(channel, channelPosition, memory, language, source, target, null, null, timestamp);
    }

    public static TranslationUnitMessage tu(int channel, long channelPosition, UUID owner, long memory, LanguageDirection language, String source, String target, Date timestamp) {
        return tu(channel, channelPosition, owner, memory, language, source, target, null, null, timestamp);
    }

    public static TranslationUnitMessage tu(int channel, long channelPosition, long memory, LanguageDirection language, String source, String target, String previousSource, String previousTarget, Date timestamp) {
        return tu(channel, channelPosition, null, memory, language, source, target, previousSource, previousTarget, timestamp);
    }

    public static TranslationUnitMessage tu(int channel, long channelPosition, UUID owner, long memory, LanguageDirection language, String source, String target, String previousSource, String previousTarget, Date timestamp) {
        return new TranslationUnitMessage((short) channel, channelPosition, owner, language, language, memory,
                source, target, previousSource, previousTarget, timestamp,
                sentence(source), sentence(target), null);
    }

    // Corpus

    public static MultilingualCorpus corpus(String name, List<TranslationUnitMessage> units) {
        final HashMap<LanguageDirection, ArrayList<TranslationUnitMessage>> lang2units = new HashMap<>();
        for (TranslationUnitMessage unit : units)
            lang2units.computeIfAbsent(unit.language, key -> new ArrayList<>()).add(unit);

        return new MultilingualCorpus() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Set<LanguageDirection> getLanguages() {
                return lang2units.keySet();
            }

            @Override
            public int getLineCount(LanguageDirection language) {
                return lang2units.get(language).size();
            }

            @Override
            public TUReader getContentReader() {
                return new TUReader() {

                    private int index = 0;

                    @Override
                    public TranslationUnit read() {
                        if (index < units.size()) {
                            TranslationUnitMessage unit = units.get(index++);

                            return new TranslationUnit(unit.language, unit.rawSentence, unit.rawTranslation);
                        } else {
                            return null;
                        }
                    }

                    @Override
                    public void close() {
                    }

                };
            }

            @Override
            public TUWriter getContentWriter(boolean append) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Corpus getCorpus(LanguageDirection language, boolean source) {
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

    public static Map<Short, Long> channels(long channel0, long channel1) {
        Map<Short, Long> result = new HashMap<>(1);
        result.put((short) 0, channel0);
        result.put((short) 1, channel1);
        return result;
    }

    // Deletion

    public static DeletionMessage deletion(long memory) {
        return new DeletionMessage((short) 1, 0, memory);
    }

    public static DeletionMessage deletion(long channelPosition, long memory) {
        return new DeletionMessage((short) 1, channelPosition, memory);
    }
}
