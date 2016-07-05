package eu.modernmt.model.impl;

import eu.modernmt.constants.Const;
import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

/**
 * Created by davide on 05/07/16.
 */
public class ParallelPropertiesCorpus implements BilingualCorpus {

    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private final File source;
    private final File target;
    private final PropertiesCorpus sourceCorpus;
    private final PropertiesCorpus targetCorpus;

    private int lineCount = -1;

    public ParallelPropertiesCorpus(Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this(FilenameUtils.removeExtension(source.getName()), sourceLanguage, source, targetLanguage, target);
    }

    public ParallelPropertiesCorpus(String name, Locale sourceLanguage, File source, Locale targetLanguage, File target) {
        this.name = name;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.source = source;
        this.target = target;

        this.sourceCorpus = new PropertiesCorpus(name, source, sourceLanguage);
        this.targetCorpus = new PropertiesCorpus(name, target, targetLanguage);
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
        return new BilingualPropertiesReader(source, target);
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

    private static final class BilingualPropertiesReader implements BilingualLineReader {

        private static Properties readProperties(File file) throws IOException {
            Reader reader = null;
            Properties properties;

            try {
                reader = new InputStreamReader(new FileInputStream(file), Const.charset.get());

                properties = new Properties();
                properties.load(reader);
            } finally {
                IOUtils.closeQuietly(reader);
            }

            return properties;
        }

        private final ArrayList<StringPair> pairs;
        private final Iterator<StringPair> iterator;

        public BilingualPropertiesReader(File sourceFile, File targetFile) throws IOException {
            Properties source = readProperties(sourceFile);
            Properties target = readProperties(targetFile);

            pairs = new ArrayList<>(source.size());
            for (String key : source.stringPropertyNames()) {
                String sourceLine = source.getProperty(key);
                String targetLine = target.getProperty(key, null);

                if (targetLine == null)
                    throw new IOException("Invalid parallel roperties file " + targetFile + ": missing key " + key);

                pairs.add(new StringPair(sourceLine, targetLine));
            }

            iterator = pairs.iterator();
        }

        @Override
        public StringPair read() {
            return iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public void close() {
            // Nothing to do
        }
    }
}
