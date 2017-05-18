package eu.modernmt.training.preprocessing;

import eu.modernmt.io.DefaultCharset;
import eu.modernmt.io.LineWriter;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.io.UnixLineWriter;
import org.apache.commons.io.FileUtils;
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

    public TermsCollectorWriter(File file) {
        this.terms = new ConcurrentHashMap<>(1000000);
        this.file = file;
    }

    @Override
    protected void doWrite(String[][] batch, LineWriter writer) throws IOException {
        StringBuilder builder = new StringBuilder();

        for (String[] line : batch) {
            for (int i = 0; i < line.length; i++) {
                String token = TokensOutputStream.escapeWhitespaces(line[i]);
                terms.put(token, Boolean.TRUE);

                if (i > 0)
                    builder.append(' ');
                builder.append(token);
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
