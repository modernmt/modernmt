package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.constants.Const;
import eu.modernmt.io.LineReader;
import eu.modernmt.io.LineWriter;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.io.UnixLineWriter;
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
        return new UnixLineReader(new FileInputStream(file), Const.charset.get());
    }

    @Override
    public LineWriter getContentWriter(boolean append) throws IOException {
        return new UnixLineWriter(new FileOutputStream(file, append), Const.charset.get());
    }

    @Override
    public Reader getRawContentReader() throws IOException {
        return new InputStreamReader(new FileInputStream(file), Const.charset.get());
    }

    @Override
    public String toString() {
        return name + "." + language;
    }

}
