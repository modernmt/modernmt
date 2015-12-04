package eu.modernmt.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

/**
 * Created by davide on 10/07/15.
 */
public class FileCorpus implements Corpus {

    private File file;
    private String name;
    private String lang;

    private static String getName(File file) {
        String fullname = file.getName();
        int lastDot = fullname.lastIndexOf('.');
        return fullname.substring(0, lastDot);
    }

    private static String getLang(File file) {
        String fullname = file.getName();
        int lastDot = fullname.lastIndexOf('.');
        return fullname.substring(lastDot + 1);
    }

    public FileCorpus(File file) {
        this(file, getName(file));
    }

    public FileCorpus(File file, String name) {
        this(file, name, getLang(file));
    }

    public FileCorpus(File file, String name, String lang) {
        this.file = file;
        this.name = name;
        this.lang = lang;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getLanguage() {
        return lang;
    }

    @Override
    public Reader getContentReader() throws FileNotFoundException {
        return new FileReader(file);
    }

    @Override
    public String toString() {
        return name + "." + lang;
    }
}
