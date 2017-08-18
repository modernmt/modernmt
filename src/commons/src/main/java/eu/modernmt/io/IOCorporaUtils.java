package eu.modernmt.io;

import eu.modernmt.model.corpus.MultilingualCorpus;
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
    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, long pairsLimit, boolean append) throws IOException {
        MultilingualCorpus.MultilingualLineReader reader = null;
        MultilingualCorpus.MultilingualLineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);

            MultilingualCorpus.StringPair pair;
            long pairs = 0;
            while ((pair = reader.read()) != null && pairs++ < pairsLimit)
                writer.write(pair);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, long linesLimit) throws IOException {
        copy(source, destination, linesLimit, false);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination, boolean append) throws IOException {
        copy(source, destination, Long.MAX_VALUE, append);
    }

    public static void copy(MultilingualCorpus source, MultilingualCorpus destination) throws IOException {
        copy(source, destination, Long.MAX_VALUE);
    }

}
