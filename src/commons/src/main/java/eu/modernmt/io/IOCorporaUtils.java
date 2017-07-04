package eu.modernmt.io;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Created by davide on 04/05/17.
 */
public class IOCorporaUtils {

    // Monolingual corpus copy
    public static void copy(Corpus source, Corpus destination, long linesLimit, boolean append) throws IOException {
        LineReader reader = null;
        LineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);
            String line;
            long lines = 0;
            while ((line = reader.readLine()) != null && lines++ < linesLimit)
                writer.writeLine(line);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(Corpus source, Corpus destination, long linesLimit) throws IOException {
        copy(source, destination, linesLimit, false);
    }

    public static void copy(Corpus source, Corpus destination, boolean append) throws IOException {
        copy(source, destination, Long.MAX_VALUE, append);
    }

    public static void copy(Corpus source, Corpus destination) throws IOException {
        copy(source, destination, Long.MAX_VALUE);
    }

    // Bilingual corpus copy
    public static void copy(BilingualCorpus source, BilingualCorpus destination, long pairsLimit, boolean append) throws IOException {
        BilingualCorpus.BilingualLineReader reader = null;
        BilingualCorpus.BilingualLineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);

            BilingualCorpus.StringPair pair;
            long pairs = 0;
            while ((pair = reader.read()) != null && pairs++ < pairsLimit)
                writer.write(pair);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(BilingualCorpus source, BilingualCorpus destination, long linesLimit) throws IOException {
        copy(source, destination, linesLimit, false);
    }

    public static void copy(BilingualCorpus source, BilingualCorpus destination, boolean append) throws IOException {
        copy(source, destination, Long.MAX_VALUE, append);
    }

    public static void copy(BilingualCorpus source, BilingualCorpus destination) throws IOException {
        copy(source, destination, Long.MAX_VALUE);
    }

}
