package eu.modernmt.io;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

/**
 * Created by davide on 04/05/17.
 */
public class IOCorporaUtils {

    public static void copy(Corpus source, Corpus destination) throws IOException {
        copy(source, destination, false);
    }

    public static void copy(Corpus source, Corpus destination, boolean append) throws IOException {
        LineReader reader = null;
        LineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);

            String line;
            while ((line = reader.readLine()) != null)
                writer.writeLine(line);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    public static void copy(BilingualCorpus source, BilingualCorpus destination) throws IOException {
        copy(source, destination, false);
    }

    public static void copy(BilingualCorpus source, BilingualCorpus destination, boolean append) throws IOException {
        BilingualCorpus.BilingualLineReader reader = null;
        BilingualCorpus.BilingualLineWriter writer = null;

        try {
            reader = source.getContentReader();
            writer = destination.getContentWriter(append);

            BilingualCorpus.StringPair pair;
            while ((pair = reader.read()) != null)
                writer.write(pair);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

}
