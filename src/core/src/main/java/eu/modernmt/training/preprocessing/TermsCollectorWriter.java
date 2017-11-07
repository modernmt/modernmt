package eu.modernmt.training.preprocessing;

import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.LineWriter;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.io.UnixLineWriter;
import eu.modernmt.model.Sentence;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by davide on 22/08/16.
 */
public class TermsCollectorWriter extends CorpusWriter {

    private final ConcurrentHashMap<String, Boolean> terms;
    private final File file;

    public TermsCollectorWriter(File file) {
        this.terms = new ConcurrentHashMap<>(1000000);
        this.file = file;
    }

    @Override
    protected void doWrite(Sentence[] batch, LineWriter writer) throws IOException {
        StringBuilder builder = new StringBuilder();

        for (Sentence sentence : batch) {
            String[] line = TokensOutputStream.tokens(sentence, false, true);

            for (int i = 0; i < line.length; i++) {
                terms.put(line[i], Boolean.TRUE);

                if (i > 0)
                    builder.append(' ');
                builder.append(line[i]);
            }

            writer.writeLine(builder.toString());
            builder.setLength(0);
        }
    }

    @Override
    public void flush() throws IOException {
        File parent = file.getParentFile();
        if (!parent.isDirectory())
            FileUtils.forceMkdir(parent);

        String[] words = terms.keySet().toArray(new String[terms.size()]);

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
