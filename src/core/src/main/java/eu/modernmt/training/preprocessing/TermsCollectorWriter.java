package eu.modernmt.training.preprocessing;

import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.LineWriter;
import eu.modernmt.io.UnixLineWriter;
import eu.modernmt.vocabulary.Vocabulary;
import eu.modernmt.vocabulary.rocksdb.RocksDBVocabulary;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 22/08/16.
 */
public class TermsCollectorWriter extends CorpusWriter {

    private final ConcurrentHashMap<String, Boolean> terms;
    private final File file;
    private final Vocabulary vocabulary;

    public TermsCollectorWriter(File file) {
        if (file.exists()) {
            this.terms = null;
            this.file = null;
            try {
                this.vocabulary = new RocksDBVocabulary(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.terms = new ConcurrentHashMap<>(1000000);
            this.file = file;
            this.vocabulary = null;
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
            for (String[] line : batch)
                for (String word : line)
                    terms.put(word, Boolean.TRUE);
        }
    }

    @Override
    public void flush() throws IOException {
        if (vocabulary == null) {
            String[] words = terms.keySet().toArray(new String[terms.size()]);

            // TODO: not necessary, but there is a bug here -> vocabulary order changes quality
            Arrays.sort(words);

            UnixLineWriter writer = null;

            try {
                writer = new UnixLineWriter(new FileOutputStream(file, false), DefaultCharset.get());

                for (String word : words)
                    writer.writeLine(word);
            } finally {
                IOUtils.closeQuietly(writer);
            }

        }
    }
}
