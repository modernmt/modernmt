package eu.modernmt.model;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 10/07/15.
 */
public class FileCorpus implements Corpus {

    public static List<FileCorpus> list(File folder, String lang) throws IOException {
        if (!folder.isDirectory())
            throw new IOException(folder + " is not a valid folder");

        Collection<File> files = FileUtils.listFiles(folder, lang == null ? null : new String[]{lang}, false);
        ArrayList<FileCorpus> corpora = new ArrayList<>(files.size());
        for (File file : files) {
            corpora.add(new FileCorpus(file));
        }

        return corpora;
    }

    private File file;
    private String name;
    private Locale lang;

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
        this.lang = Locale.forLanguageTag(lang);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Locale getLanguage() {
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
