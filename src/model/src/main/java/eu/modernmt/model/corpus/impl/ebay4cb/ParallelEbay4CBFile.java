package eu.modernmt.model.corpus.impl.ebay4cb;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.Corpus;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 14/03/16.
 */
public class ParallelEbay4CBFile implements BilingualCorpus {

    private final File source;
    private final File target;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private final Ebay4CBCorpus sourceCorpus;
    private final Ebay4CBCorpus targetCorpus;

    private int lineCount = -1;

    public ParallelEbay4CBFile(Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this(FilenameUtils.removeExtension(source.getName()), sourceLanguage, source, targetLanguage, target);
    }

    public ParallelEbay4CBFile(String name, Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.source = source;
        this.target = target;

        this.sourceCorpus = new Ebay4CBCorpus(name, sourceLanguage, source);
        this.targetCorpus = new Ebay4CBCorpus(name, targetLanguage, target);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getSourceLanguage() {
        return sourceLanguage;
    }

    @Override
    public Locale getTargetLanguage() {
        return targetLanguage;
    }

    @Override
    public int getLineCount() throws IOException {
        if (lineCount < 0) {
            synchronized (this) {
                if (lineCount < 0) {
                    this.lineCount = BilingualCorpus.getLineCount(this);
                }
            }
        }

        return this.lineCount;
    }

    @Override
    public BilingualLineReader getContentReader() throws IOException {
        return new Parallel4CBFileLineReader(source, target);
    }

    @Override
    public BilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Corpus getSourceCorpus() {
        return sourceCorpus;
    }

    @Override
    public Corpus getTargetCorpus() {
        return targetCorpus;
    }

    @Override
    public String toString() {
        return name + ".4cb{" + sourceLanguage.toLanguageTag() + '|' + targetLanguage.toLanguageTag() + '}';
    }

    private static class Parallel4CBFileLineReader implements BilingualLineReader {

        private final ArrayList<StringPair> pairs;
        private final Iterator<StringPair> iterator;

        private Parallel4CBFileLineReader(File source, File target) throws IOException {
            Map<String, String> flattenedSource = getFlattened(source);
            Map<String, String> flattenedTarget = getFlattened(target);

            pairs = new ArrayList<>(flattenedSource.size());

            for (Map.Entry<String, String> entry : flattenedSource.entrySet()) {
                String sourceLine = entry.getValue();
                String targetLine = flattenedTarget.get(entry.getKey());

                if (targetLine == null)
                    throw new IOException("Not parallel files: " + source + ", " + target);

                pairs.add(new StringPair(sourceLine, targetLine));
            }

            iterator = pairs.iterator();
        }

        private static Map<String, String> getFlattened(File file) throws IOException {
            HashMap<String, String> map = new HashMap<>();
            Ebay4CBFileReader reader = null;

            try {
                reader = new Ebay4CBFileReader(file);

                Ebay4CBFileReader.Line4CB element;
                while ((element = reader.readLineWithMetadata()) != null) {
                    map.put(element.path, element.line);
                }
            } finally {
                IOUtils.closeQuietly(reader);
            }

            return map;
        }

        @Override
        public StringPair read() throws IOException {
            return iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public void close() {
            // Nothing to do
        }
    }

}
