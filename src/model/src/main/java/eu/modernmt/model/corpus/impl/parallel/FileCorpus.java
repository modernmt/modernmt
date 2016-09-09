package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.*;
import eu.modernmt.model.corpus.Corpus;

import java.io.*;
import java.util.Locale;

/**
 * Created by davide on 10/07/15.
 */
public class FileCorpus implements Corpus {

    private File file;
    private String name;
    private Locale language;

    private static String getNameFromFile(File file) {
        String fullname = file.getName();
        int lastDot = fullname.lastIndexOf('.');
        return fullname.substring(0, lastDot);
    }

    private static Locale getLangFromFile(File file) {
        String fullname = file.getName();
        int lastDot = fullname.lastIndexOf('.');
        return Locale.forLanguageTag(fullname.substring(lastDot + 1));
    }

    public FileCorpus(File file) {
        this(file, null);
    }

    public FileCorpus(File file, String name) {
        this(file, name, null);
    }

    public FileCorpus(File file, String name, Locale language) {
        this.file = file;
        this.name = (name == null ? getNameFromFile(file) : name);
        this.language = (language == null ? getLangFromFile(file) : language);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public LineReader getContentReader() throws FileNotFoundException {
        return new UnixLineReader(new FileInputStream(file), DefaultCharset.get());
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        return new UnixLineWriter(new FileOutputStream(file, append), DefaultCharset.get());
    }

    @Override
    public Reader getRawContentReader() throws IOException {
        return new InputStreamReader(new FileInputStream(file), DefaultCharset.get());
    }

    @Override
    public String toString() {
        return name + "." + language;
    }

}
