package eu.modernmt.model.util;

import eu.modernmt.model.BilingualCorpus;
import eu.modernmt.model.Corpus;
import eu.modernmt.model.impl.BilingualFileCorpus;
import eu.modernmt.model.impl.FileCorpus;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 24/02/16.
 */
public class CorpusUtils {

    public static final String TMX_EXTENSION = "tmx";

    public static List<Corpus> list(Locale language, File... roots) throws IOException {
        String tag = language.toLanguageTag();

        ArrayList<Corpus> corpora = new ArrayList<>();

        for (File folder : roots) {
            if (!folder.isDirectory())
                throw new IOException(folder + " is not a valid folder");

            Collection<File> files = FileUtils.listFiles(folder, new String[]{tag}, false);
            corpora.addAll(files.stream().map(FileCorpus::new).collect(Collectors.toList()));
        }

        return corpora;
    }

    public static void list(Collection<Corpus> monolingualOutput, boolean monolingualIsTarget, Collection<BilingualCorpus> bilingualOutput, Locale sourceLanguage, Locale targetLanguage, File... roots) throws IOException {
        String sourceLangTag = sourceLanguage.toLanguageTag();
        String targetLangTag = targetLanguage.toLanguageTag();
        String monolingualLangTag = monolingualIsTarget ? targetLangTag : sourceLangTag;

        String[] acceptedExtensions = new String[]{sourceLangTag, targetLangTag, TMX_EXTENSION};

        for (File directory : roots) {
            HashMap<String, File> name2File = new HashMap<>();

            for (File file : FileUtils.listFiles(directory, acceptedExtensions, false)) {
                String filename = file.getName();

                int lastDot = filename.lastIndexOf('.');
                if (lastDot < 0)
                    continue;

                String extension = filename.substring(lastDot + 1);
                filename = filename.substring(0, lastDot);

                if (filename.isEmpty() || extension.isEmpty())
                    continue;

                if (TMX_EXTENSION.equalsIgnoreCase(extension)) {
                    // Ignore for now
                    continue;
                } else {
                    File twin = name2File.get(filename);

                    if (twin == null) {
                        name2File.put(filename, file);
                    } else {
                        name2File.remove(filename);
                        bilingualOutput.add(new BilingualFileCorpus(directory, filename, sourceLanguage, targetLanguage));
                    }
                }
            }

            for (File file : name2File.values()) {
                String filename = file.getName();

                int lastDot = filename.lastIndexOf('.');

                String extension = filename.substring(lastDot + 1);
                filename = filename.substring(0, lastDot);

                if (monolingualLangTag.equalsIgnoreCase(extension))
                    monolingualOutput.add(new FileCorpus(file, filename, monolingualIsTarget ? targetLanguage : sourceLanguage));
            }
        }

    }

}
