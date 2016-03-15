package eu.modernmt.model.impl.tmx;

import eu.modernmt.model.BilingualCorpus;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class TMXFile implements BilingualCorpus {

    private final File tmx;
    private final String name;
    private final Locale sourceLanguage;
    private final Locale targetLanguage;
    private int lineCount = -1;

    private final TMXView sourceCorpus;
    private final TMXView targetCorpus;

    public TMXFile(File tmx, Locale sourceLanguage, Locale targetLanguage) {
        this(FilenameUtils.removeExtension(tmx.getName()), tmx, sourceLanguage, targetLanguage);
    }

    public TMXFile(String name, File tmx, Locale sourceLanguage, Locale targetLanguage) {
        this.targetLanguage = targetLanguage;
        this.sourceLanguage = sourceLanguage;
        this.name = name;
        this.tmx = tmx;

        this.sourceCorpus = new TMXView(tmx, name, sourceLanguage);
        this.targetCorpus = new TMXView(tmx, name, targetLanguage);
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
    public BilingualStringReader getContentReader() throws IOException {
        return new TMXBilingualStringReader(tmx, sourceLanguage, targetLanguage);
    }

    @Override
    public BilingualStringWriter getContentWriter(boolean append) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TMXView getSourceCorpus() {
        return sourceCorpus;
    }

    @Override
    public TMXView getTargetCorpus() {
        return targetCorpus;
    }

    @Override
    public String toString() {
        return name + ".tmx{" + sourceLanguage.toLanguageTag() + '|' + targetLanguage.toLanguageTag() + '}';
    }

}
