package eu.modernmt.decoder.opennmt.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * Created by davide on 03/08/17.
 */
public class TestUtils {

    private static long channelPosition = 1;

    public static Sentence sentence(String text) {
        String[] tokens = text.split("\\s+");
        Word[] words = new Word[tokens.length];

        for (int i = 0; i < words.length; i++)
            words[i] = new Word(tokens[i], " ");

        return new Sentence(words);
    }

    public static TranslationUnit tu(LanguagePair language, String source, String target) {
        TranslationUnit tu = new TranslationUnit((short) 0, channelPosition++, language, 1L, source, target);
        tu.sourceSentence = sentence(source);
        tu.targetSentence = sentence(target);
        return tu;
    }

    public static MultilingualCorpus corpus(LanguagePair language, String... sourceAndTargets) {
        return new MultilingualCorpus() {
            @Override
            public String getName() {
                return "dummy";
            }

            @Override
            public Set<LanguagePair> getLanguages() {
                return Collections.singleton(language);
            }

            @Override
            public int getLineCount(LanguagePair language) {
                return sourceAndTargets.length / 2;
            }

            @Override
            public MultilingualLineReader getContentReader() throws IOException {
                return new MultilingualLineReader() {

                    private int index = 0;

                    @Override
                    public StringPair read() throws IOException {
                        if (index < sourceAndTargets.length) {
                            String source = sourceAndTargets[index];
                            String target = sourceAndTargets[index + 1];
                            index += 2;

                            return new StringPair(language, source, target);
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
}
