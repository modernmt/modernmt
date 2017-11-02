package eu.modernmt.training;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.MultilingualCorpusWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by davide on 28/08/17.
 */
public class ReducedMultilingualCorpus extends BaseMultilingualCorpus implements MultilingualCorpusWrapper {

    private final MultilingualCorpus corpus;
    private final HashMap<LanguagePair, Double> reductions = new HashMap<>();

    public ReducedMultilingualCorpus(MultilingualCorpus corpus) {
        this.corpus = corpus;
    }

    public void reduce(LanguagePair language, double reduction) {
        reductions.put(language, reduction);
    }

    @Override
    public MultilingualCorpus getWrappedCorpus() {
        return corpus;
    }

    @Override
    public String getName() {
        return corpus.getName();
    }

    private Random newRandom() {
        return new Random(corpus.getName().hashCode());
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        return new MultilingualLineReader() {

            private final Random random = newRandom();
            private final MultilingualLineReader reader = corpus.getContentReader();

            @Override
            public StringPair read() throws IOException {
                while (true) {
                    StringPair pair = reader.read();

                    if (pair == null)
                        return null;

                    Double reduction = reductions.get(pair.language);

                    if (test(random, reduction))
                        return pair;
                }
            }

            @Override
            public void close() throws IOException {
                reader.close();
            }
        };
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new MultilingualLineWriter() {

            private final Random random = newRandom();
            private final MultilingualLineWriter writer = corpus.getContentWriter(append);

            @Override
            public void write(StringPair pair) throws IOException {
                Double reduction = reductions.get(pair.language);

                if (test(random, reduction))
                    writer.write(pair);
            }

            @Override
            public void flush() throws IOException {
                writer.flush();
            }

            @Override
            public void close() throws IOException {
                writer.close();
            }
        };
    }

    private static boolean test(Random random, Double reduction) {
        double test = random.nextDouble();
        return reduction == null || (test < reduction);
    }

}
