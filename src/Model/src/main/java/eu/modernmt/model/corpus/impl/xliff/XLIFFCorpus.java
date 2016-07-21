package eu.modernmt.model.corpus.impl.xliff;

import eu.modernmt.model.corpus.BilingualCorpus;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 18/07/16.
 */
public class XLIFFCorpus implements BilingualCorpus {

    private final File xliff;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private int lineCount = -1;

    private final XLIFFMonolingualView sourceCorpus;
    private final XLIFFMonolingualView targetCorpus;

    public XLIFFCorpus(File xliff, Locale sourceLanguage, Locale targetLanguage) {
        this(FilenameUtils.removeExtension(xliff.getName()), xliff, sourceLanguage, targetLanguage);
    }

    public XLIFFCorpus(String name, File xliff, Locale sourceLanguage, Locale targetLanguage) {
        this.targetLanguage = targetLanguage;
        this.sourceLanguage = sourceLanguage;
        this.name = name;
        this.xliff = xliff;

        this.sourceCorpus = new XLIFFMonolingualView(xliff, name, sourceLanguage);
        this.targetCorpus = new XLIFFMonolingualView(xliff, name, targetLanguage);
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
        return new XLIFFBilingualLineReader(xliff, sourceLanguage, targetLanguage);
    }

    @Override
    public BilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public XLIFFMonolingualView getSourceCorpus() {
        return sourceCorpus;
    }

    @Override
    public XLIFFMonolingualView getTargetCorpus() {
        return targetCorpus;
    }

    @Override
    public String toString() {
        return name + ".xliff{" + sourceLanguage.toLanguageTag() + '|' + targetLanguage.toLanguageTag() + '}';
    }

}
