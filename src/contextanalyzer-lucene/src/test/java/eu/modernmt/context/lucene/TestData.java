package eu.modernmt.context.lucene;

import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.LineReader;
import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 05/08/17.
 */
public class TestData {

    public static final Language2 EN = Language2.fromString("en");
    public static final Language2 EN_US = Language2.fromString("en-US");
    public static final Language2 FR = Language2.fromString("fr");
    public static final Language2 FR_CA = Language2.fromString("fr-CA");
    public static final Language2 IT = Language2.fromString("it");
    public static final Language2 IT_CH = Language2.fromString("it-CH");

    public static final LanguageDirection FR__EN = new LanguageDirection(TestData.FR, TestData.EN);
    public static final LanguageDirection EN__IT = new LanguageDirection(TestData.EN, TestData.IT);
    public static final LanguageDirection EN__FR = new LanguageDirection(TestData.EN, TestData.FR);
    public static final LanguageDirection IT__EN = new LanguageDirection(TestData.IT, TestData.EN);
    public static final LanguageDirection EN_US__IT = new LanguageDirection(TestData.EN_US, TestData.IT);
    public static final LanguageDirection EN_US__IT_CH = new LanguageDirection(TestData.EN_US, TestData.IT_CH);
    public static final LanguageDirection IT_CH__EN_US = new LanguageDirection(TestData.IT_CH, TestData.EN_US);
    public static final LanguageDirection IT__EN_US = new LanguageDirection(TestData.IT, TestData.EN_US);
    public static final LanguageDirection FR_CA__EN_US = new LanguageDirection(TestData.FR_CA, TestData.EN_US);

    private static final HashMap<String, String> EXAMPLE_SENTENCES = new HashMap<>();
    private static final HashMap<String, String> EXAMPLE_CONTENTS = new HashMap<>();
    private static final HashMap<String, Set<String>> EXAMPLE_TERMS = new HashMap<>();

    static {
        EXAMPLE_SENTENCES.put("en", "hello world");
        EXAMPLE_SENTENCES.put("it", "ciao mondo");
        EXAMPLE_SENTENCES.put("fr", "bonjour monde");

        EXAMPLE_CONTENTS.put("en", "If Parliament and the Commission work together real progress can be made in 'cleaning up' the Commission.\n" +
                "If this does not happen the college of Commissioners will cease to have my support.\n" +
                "He who is not punished for a greater crime will be punished for a lesser crime.\n" +
                "Contaminated feed continued to be in circulation, and the European Commission still said nothing.\n" +
                "Several tens of cases of Creutzfeldt-Jacob's disease were declared, condemning young Europeans because the cattle disease had perhaps been transmitted to humans.\n" +
                "In any case, the risk was knowingly taken to contaminate millions of human beings.\n" +
                "This was done by the European Commission. This health risk was taken by the Commission.\n" +
                "Let us not mince our words: in this affair the Commission's attitude was more than negligent, it was an irresponsible and criminal attitude.\n" +
                "Yet the European Parliament did not punish the Commission.\n" +
                "There was no punishment in this case either.");
        EXAMPLE_CONTENTS.put("it", "Se Parlamento e Commissione collaboreranno, potremo veramente «ripulire» la Commissione.\n" +
                "Altrimenti non offrirò più il mio sostegno al collegio dei Commissari.\n" +
                "Chi non è censurato nel grande, viene censurato nel piccolo.\n" +
                "Sono ancora in circolazione farine contaminate e la Commissione europea non dice nulla.\n" +
                "Sono state denunciate decine di casi della sindrome di Creutzfeldt-Jacob, che ha colpito giovani europei, perché la malattia della mucca pazza può essere trasmessa all'uomo.\n" +
                "In definitiva, si è corso volontariamente il rischio di contaminare milioni di esseri umani.\n" +
                "Ecco cosa ha fatto la Commissione europea: ha corso un tale rischio sanitario.\n" +
                "Chiamiamo le cose con il loro nome: la Commissione in questo caso non è stata negligente, ma ha avuto un atteggiamento totalmente irresponsabile e criminale.\n" +
                "Il Parlamento europeo non ha preso provvedimenti all'epoca.\n" +
                "Anche in questo caso, non vi fu alcuna sanzione.");
        EXAMPLE_CONTENTS.put("fr", "Si le Parlement et la Commission travaillent ensemble, des progrès réels peuvent être réalisés dans le «nettoyage» de la Commission.\n" +
                "Si cela n'arrive pas, le collège des commissaires cessera d'avoir mon soutien.\n" +
                "Celui qui n'est pas puni pour un crime majeur sera puni pour un crime de moindre importance.\n" +
                "Les aliments contaminés continuent d'être en circulation, et la Commission européenne n'a toujours rien dit.\n" +
                "Plusieurs dizaines de cas de maladie de Creutzfeldt-Jacob ont été déclarés, condamnant les jeunes Européens parce que la maladie du bétail avait peut-être été transmise aux humains.\n" +
                "En tout cas, le risque a été sciemment pris pour contaminer des millions d'êtres humains.\n" +
                "Cela a été fait par la Commission européenne. Ce risque pour la santé a été pris par la Commission.\n" +
                "Ne tenons pas nos mots: dans cette affaire, l'attitude de la Commission était plus que négligente, c'était une attitude irresponsable et criminelle.\n" +
                "Pourtant, le Parlement européen n'a pas puni la Commission.\n" +
                "Il n'y a pas eu de châtiment dans ce cas non plus.");

        EXAMPLE_TERMS.put("en", new HashSet<>(Arrays.asList(
                ("college been creutzfeldt mince several had parliament transmitted negligent either lesser punish than let " +
                        "affair up because criminal greater case us happen still work made european nothing cattle real my " +
                        "done condemning feed were europeans contaminated millions support who attitude tens young our can " +
                        "perhaps does taken irresponsible have commission crime punished said jacob human together contaminate " +
                        "cleaning declared commissioners cases disease more words continued health yet any beings punishment " +
                        "cease circulation progress risk he humans did knowingly").split(" ")
        )));
        EXAMPLE_TERMS.put("it", new HashSet<>(Arrays.asList(
                ("contaminare irresponsabile negligente creutzfeldt censurato stata dice europea commissari ancora uomo " +
                        "può tale cose provvedimenti offrirò cosa caso definitiva parlamento circolazione sanitario ripulire " +
                        "casi state chiamiamo fatto sindrome volontariamente commissione ecco nome colpito totalmente " +
                        "altrimenti epoca criminale denunciate corso pazza trasmessa essere potremo rischio giovani malattia " +
                        "esseri jacob contaminate collegio preso milioni umani sostegno grande mucca piccolo europei " +
                        "atteggiamento farine veramente collaboreranno alcuna sanzione viene europeo decine nulla").split(" ")
        )));
    }

    public static Sentence sentence(String text) {
        String[] tokens = text.split("\\s+");
        Word[] words = new Word[tokens.length];

        for (int i = 0; i < words.length; i++)
            words[i] = new Word(tokens[i], " ");

        return new Sentence(words);
    }

    // Translation units

    public static List<TranslationUnit> tuList(LanguageDirection language, int size) {
        return tuList(1L, language, size);
    }

    public static List<TranslationUnit> tuList(long memory, LanguageDirection language, int size) {
        return tuList(0, 0L, memory, language, size);
    }

    public static List<TranslationUnit> tuList(int channel, long channelPosition, long memory, LanguageDirection language, int size) {
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

    public static TranslationUnit tu(LanguageDirection language, Date timestamp) {
        return tu(1L, language, timestamp);
    }

    public static TranslationUnit tu(long memory, LanguageDirection language, Date timestamp) {
        return tu(0, 0L, memory, language, timestamp);
    }

    public static TranslationUnit tu(int channel, long channelPosition, long memory, LanguageDirection language, Date timestamp) {
        return tu(channel, channelPosition, memory, language,
                EXAMPLE_SENTENCES.get(language.source.getLanguage()),
                EXAMPLE_SENTENCES.get(language.target.getLanguage()),
                timestamp);
    }

    public static TranslationUnit tu(LanguageDirection language, String source, String target, Date timestamp) {
        return tu(1L, language, source, target, timestamp);
    }

    public static TranslationUnit tu(long memory, LanguageDirection language, String source, String target, Date timestamp) {
        return tu(0, 0, memory, language, source, target, timestamp);
    }

    public static TranslationUnit tu(int channel, long channelPosition, long memory, LanguageDirection language, String source, String target, Date timestamp) {
        return tu(channel, channelPosition, null, memory, language, source, target, timestamp);
    }

    public static TranslationUnit tu(int channel, long channelPosition, UUID owner, long memory, LanguageDirection language, String source, String target, Date timestamp) {
        return new TranslationUnit((short) channel, channelPosition, owner, language, memory,
                source, target, null, null, timestamp,
                sentence(source), sentence(target), null);
    }

    public static Set<String> tuGetTerms(List<TranslationUnit> units, boolean source) throws IOException {
        return tuGetTerms(units, source, null);
    }

    public static Set<String> tuGetTerms(List<TranslationUnit> units, boolean source, LanguageDirection direction) throws IOException {
        HashSet<String> terms = new HashSet<>();
        for (TranslationUnit unit : units) {
            if (direction == null || unit.direction.equals(direction)) {
                String text = source ? unit.rawSentence : unit.rawTranslation;
                terms.addAll(Arrays.asList(text.split(" ")));
            }
        }

        return terms;
    }

    public static String tuGetContent(List<TranslationUnit> units, boolean source) {
        return tuGetContent(units, source, null);
    }

    public static String tuGetContent(List<TranslationUnit> units, boolean source, LanguageDirection direction) {
        StringBuilder builder = new StringBuilder();
        for (TranslationUnit unit : units) {
            if (direction == null || unit.direction.equals(direction)) {
                builder.append(source ? unit.rawSentence : unit.rawTranslation);
                builder.append('\n');
            }
        }

        return builder.substring(0, builder.length() - 1);
    }

    // Channels

    public static Map<Short, Long> channels(int channel, long position) {
        Map<Short, Long> result = new HashMap<>(1);
        result.put((short) channel, position);
        return result;
    }

    public static Map<Short, Long> channels(long channel0, long channel1) {
        Map<Short, Long> result = new HashMap<>(2);
        result.put((short) 0, channel0);
        result.put((short) 1, channel1);
        return result;
    }

    // Deletion

    public static Deletion deletion(long memory) {
        return new Deletion((short) 1, 0, memory);
    }

    public static Deletion deletion(long channelPosition, long memory) {
        return new Deletion((short) 1, channelPosition, memory);
    }

    // Content

    public static String getContent(Language2... locales) {
        StringBuilder builder = new StringBuilder();
        for (Language2 locale : locales) {
            builder.append(EXAMPLE_CONTENTS.get(locale.getLanguage()));
            builder.append('\n');
        }

        return builder.substring(0, builder.length() - 1);
    }

    public static Set<String> getTerms(Language2... locales) {
        HashSet<String> terms = new HashSet<>();
        for (Language2 locale : locales)
            terms.addAll(EXAMPLE_TERMS.get(locale.getLanguage()));

        return terms;
    }

    // Corpora

    public static DummyBilingualCorpus corpus(String name, LanguageDirection direction) {
        return corpus(name, direction,
                EXAMPLE_CONTENTS.get(direction.source.getLanguage()),
                EXAMPLE_CONTENTS.get(direction.target.getLanguage()));
    }

    public static DummyBilingualCorpus corpus(String name, LanguageDirection direction, String sourceContent, String targetContent) {
        return new DummyBilingualCorpus(
                new StringCorpus(name, direction.source, sourceContent),
                new StringCorpus(name, direction.target, targetContent));
    }

    public static DummyMultilingualCorpus corpus(List<DummyBilingualCorpus> corpora) {
        return new DummyMultilingualCorpus(corpora);
    }

    public static DummyMultilingualCorpus corpus(DummyBilingualCorpus... corpora) {
        return new DummyMultilingualCorpus(corpora);
    }

    public static class DummyBilingualCorpus extends BaseMultilingualCorpus {

        private final String name;
        private final LanguageDirection language;
        private final StringCorpus sourceCorpus;
        private final StringCorpus targetCorpus;

        public DummyBilingualCorpus(StringCorpus sourceCorpus, StringCorpus targetCorpus) {
            this.name = sourceCorpus.getName();
            this.language = new LanguageDirection(sourceCorpus.getLanguage(), targetCorpus.getLanguage());
            this.sourceCorpus = sourceCorpus;
            this.targetCorpus = targetCorpus;
        }

        public String getSourceCorpus() {
            return sourceCorpus.toString().trim();
        }

        public String getTargetCorpus() {
            return targetCorpus.toString().trim();
        }

        @Override
        public String getName() {
            return name;
        }

        public LanguageDirection getLanguage() {
            return language;
        }

        @Override
        public Set<LanguageDirection> getLanguages() {
            return Collections.singleton(language);
        }

        @Override
        public MultilingualLineReader getContentReader() throws IOException {
            return new MultilingualLineReader() {

                private final LineReader sourceReader = sourceCorpus.getContentReader();
                private final LineReader targetReader = targetCorpus.getContentReader();

                @Override
                public StringPair read() throws IOException {
                    String source = sourceReader.readLine();
                    String target = targetReader.readLine();

                    if (source == null && target == null)
                        return null;

                    if (source == null || target == null)
                        throw new IOException("Not-parallel string corpora");

                    return new StringPair(language, source, target);
                }

                @Override
                public void close() throws IOException {
                    IOUtils.closeQuietly(sourceReader);
                    IOUtils.closeQuietly(targetReader);
                }
            };
        }

        @Override
        public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Corpus getCorpus(LanguageDirection language, boolean source) {
            if (!language.equals(this.language))
                throw new UnsupportedLanguageException(language);

            return source ? sourceCorpus : targetCorpus;
        }
    }

    public static class DummyMultilingualCorpus extends BaseMultilingualCorpus {

        private final DummyBilingualCorpus[] corpora;

        public DummyMultilingualCorpus(DummyBilingualCorpus[] corpora) {
            this.corpora = corpora;
        }

        public DummyMultilingualCorpus(List<DummyBilingualCorpus> corpora) {
            this.corpora = corpora.toArray(new DummyBilingualCorpus[corpora.size()]);
        }

        @Override
        public String getName() {
            return corpora[0].getName();
        }

        @Override
        public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public MultilingualLineReader getContentReader() throws IOException {
            return new MultilingualLineReader() {

                private int index = 0;
                private MultilingualLineReader reader = null;

                @Override
                public StringPair read() throws IOException {
                    StringPair pair = null;

                    while (pair == null) {
                        if (reader == null) {
                            if (index < corpora.length)
                                reader = corpora[index++].getContentReader();
                            else
                                return null;
                        }

                        pair = reader.read();

                        if (pair == null) {
                            reader.close();
                            reader = null;
                        }
                    }

                    return pair;
                }

                @Override
                public void close() throws IOException {
                    if (reader != null)
                        reader.close();
                }
            };
        }

    }
}
