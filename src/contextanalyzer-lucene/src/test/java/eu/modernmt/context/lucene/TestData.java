package eu.modernmt.context.lucene;

import eu.modernmt.context.lucene.analysis.LuceneUtils;
import eu.modernmt.context.lucene.analysis.lang.LanguageAnalyzer;
import eu.modernmt.data.Deletion;
import eu.modernmt.io.LineReader;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.lang.UnsupportedLanguageException;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.impl.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.impl.StringCorpus;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;

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

    private static final HashMap<String, String> EXAMPLE_CONTENTS = new HashMap<>();

    static {
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
    }

    // Channels

    public static Map<Short, Long> channels(int channel, long position) {
        Map<Short, Long> result = new HashMap<>(1);
        result.put((short) channel, position);
        return result;
    }

    // Deletion

    public static Deletion deletion(long domain) {
        return new Deletion((short) 1, 0, domain);
    }

    public static Deletion deletion(long channelPosition, long domain) {
        return new Deletion((short) 1, channelPosition, domain);
    }

    // Content

    public static String getContent(Locale... locales) {
        StringBuilder builder = new StringBuilder();
        for (Locale locale : locales) {
            builder.append(EXAMPLE_CONTENTS.get(locale.getLanguage()));
            builder.append('\n');
        }

        return builder.substring(0, builder.length() - 1);
    }

    public static Set<String> getTerms(Locale... locales) throws IOException {
        HashSet<String> terms = new HashSet<>();
        for (Locale locale : locales) {
            Analyzer analyzer = LanguageAnalyzer.getByLanguage(locale);
            terms.addAll(LuceneUtils.analyze(analyzer, EXAMPLE_CONTENTS.get(locale.getLanguage())));
        }

        return terms;
    }

    // Corpora

    public static DummyBilingualCorpus corpus(String name, LanguagePair direction) {
        return corpus(name, direction,
                EXAMPLE_CONTENTS.get(direction.source.getLanguage()),
                EXAMPLE_CONTENTS.get(direction.target.getLanguage()));
    }

    public static DummyBilingualCorpus corpus(String name, LanguagePair direction, String sourceContent, String targetContent) {
        return new DummyBilingualCorpus(
                new StringCorpus(name, direction.source, sourceContent),
                new StringCorpus(name, direction.target, targetContent));
    }

    public static DummyMultilingualCorpus corpus(List<DummyBilingualCorpus> corpora) {
        return new DummyMultilingualCorpus(corpora);
    }

    public static DummyMultilingualCorpus corpus(DummyBilingualCorpus[] corpora) {
        return new DummyMultilingualCorpus(corpora);
    }

    public static class DummyBilingualCorpus extends BaseMultilingualCorpus {

        private final String name;
        private final LanguagePair language;
        private final StringCorpus sourceCorpus;
        private final StringCorpus targetCorpus;

        public DummyBilingualCorpus(StringCorpus sourceCorpus, StringCorpus targetCorpus) {
            this.name = sourceCorpus.getName();
            this.language = new LanguagePair(sourceCorpus.getLanguage(), targetCorpus.getLanguage());
            this.sourceCorpus = sourceCorpus;
            this.targetCorpus = targetCorpus;
        }

        @Override
        public String getName() {
            return name;
        }

        public LanguagePair getLanguage() {
            return language;
        }

        @Override
        public Set<LanguagePair> getLanguages() {
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
        public Corpus getCorpus(LanguagePair language, boolean source) {
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
