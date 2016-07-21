package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.model.corpus.BilingualCorpus;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class TMXCorpus implements BilingualCorpus {

    private final File tmx;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private int lineCount = -1;

    private final TMXMonolingualView sourceCorpus;
    private final TMXMonolingualView targetCorpus;

    public TMXCorpus(File tmx, Locale sourceLanguage, Locale targetLanguage) {
        this(FilenameUtils.removeExtension(tmx.getName()), tmx, sourceLanguage, targetLanguage);
    }

    public TMXCorpus(String name, File tmx, Locale sourceLanguage, Locale targetLanguage) {
        this.targetLanguage = targetLanguage;
        this.sourceLanguage = sourceLanguage;
        this.name = name;
        this.tmx = tmx;

        this.sourceCorpus = new TMXMonolingualView(tmx, name, sourceLanguage);
        this.targetCorpus = new TMXMonolingualView(tmx, name, targetLanguage);
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
        return new TMXBilingualLineReader(tmx, sourceLanguage, targetLanguage);
    }

    @Override
    public BilingualLineWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TMXMonolingualView getSourceCorpus() {
        return sourceCorpus;
    }

    @Override
    public TMXMonolingualView getTargetCorpus() {
        return targetCorpus;
    }

    @Override
    public String toString() {
        return name + ".tmx{" + sourceLanguage.toLanguageTag() + '|' + targetLanguage.toLanguageTag() + '}';
    }

}
