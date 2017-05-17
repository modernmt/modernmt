package eu.modernmt.training.preprocessing;

import eu.modernmt.io.LineWriter;
import eu.modernmt.vocabulary.Vocabulary;
import eu.modernmt.vocabulary.VocabularyBuilder;
import eu.modernmt.vocabulary.rocksdb.RocksDBVocabulary;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 22/08/16.
 */
public class TermsCollectorWriter extends CorpusWriter {

    private final ConcurrentHashMap<String, Boolean> terms;
    private final File file;
    private final Vocabulary vocabulary;
    private final VocabularyBuilder builder;

    public TermsCollectorWriter(File file) {
        if (file.exists()) {
            this.terms = null;
            this.file = null;
            try {
                this.vocabulary = new RocksDBVocabulary(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.builder = null;
        } else {
            this.terms = new ConcurrentHashMap<>(1000000);
            this.file = file;
            this.vocabulary = null;
            this.builder = RocksDBVocabulary.newBuilder(file);
        }
    }

    @Override
    protected void doWrite(String[][] batch, LineWriter writer) throws IOException {
        if (vocabulary != null) {
            int[][] encoded = vocabulary.lookupLines(batch, false);

            StringBuilder builder = new StringBuilder();
            for (int[] line : encoded) {
                for (int i = 0; i < line.length; i++) {
                    if (i > 0)
                        builder.append(' ');
                    builder.append(Integer.toUnsignedString(line[i]));
                }
                builder.append('\n');

                writer.writeLine(builder.toString());
                builder.setLength(0);
            }
        } else {
            builder.addLines(batch);
        }
    }

    @Override
    public void flush() throws IOException {
        if (builder != null)
            builder.build();
    }
}
