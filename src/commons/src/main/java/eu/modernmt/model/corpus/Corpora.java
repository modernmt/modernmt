package eu.modernmt.model.corpus;

import eu.modernmt.io.FileProxy;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by davide on 24/02/16.
 */
public class Corpora {

    public static final String TMX_EXTENSION = "tmx";

    public static MultilingualCorpus unwrap(MultilingualCorpus corpus) {
        while (corpus instanceof MultilingualCorpusWrapper) {
            corpus = ((MultilingualCorpusWrapper) corpus).getWrappedCorpus();
        }

        return corpus;
    }

    public static MultilingualCorpus rename(MultilingualCorpus corpus, File folder) {
        return rename(corpus, folder, corpus.getName());
    }

    public static MultilingualCorpus rename(MultilingualCorpus corpus, File folder, String name) {
        corpus = unwrap(corpus);

        if (corpus instanceof TMXCorpus) {
            return new TMXCorpus(new File(folder, name + ".tmx"));
        } else if (corpus instanceof ParallelFileCorpus) {
            LanguagePair language = ((ParallelFileCorpus) corpus).getLanguage();
            return new ParallelFileCorpus(folder, name, language);
        } else {
            throw new IllegalArgumentException("Unknown multilingual corpus: " + corpus.getClass().getSimpleName());
        }
    }

    public static long fileSize(MultilingualCorpus corpus) {
        corpus = unwrap(corpus);

        if (corpus instanceof TMXCorpus) {
            TMXCorpus tmxCorpus = (TMXCorpus) corpus;
            return fileSize(tmxCorpus.getFile());
        } else if (corpus instanceof ParallelFileCorpus) {
            ParallelFileCorpus parallelFileCorpus = ((ParallelFileCorpus) corpus);
            return fileSize(parallelFileCorpus.getSourceFile()) + fileSize(parallelFileCorpus.getTargetFile());
        } else {
            throw new IllegalArgumentException("Unknown multilingual corpus: " + corpus.getClass().getSimpleName());
        }
    }

    private static long fileSize(FileProxy proxy) {
        if (proxy instanceof FileProxy.NativeFileProxy)
            return ((FileProxy.NativeFileProxy) proxy).getFile().length();
        else
            return -1;
    }

    public static Map<LanguagePair, Integer> countLines(MultilingualCorpus corpus) throws IOException {
        Map<LanguagePair, Counter> counts = new HashMap<>();

        MultilingualCorpus.MultilingualLineReader reader = null;

        try {
            reader = corpus.getContentReader();

            MultilingualCorpus.StringPair line;
            while ((line = reader.read()) != null) {
                counts.computeIfAbsent(line.language, k -> new Counter()).count++;
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }

        Map<LanguagePair, Integer> result = new HashMap<>(counts.size());
        for (Map.Entry<LanguagePair, Counter> entry : counts.entrySet())
            result.put(entry.getKey(), entry.getValue().count);

        return result;
    }

    private static final class Counter {
        public int count = 0;
    }

    public static List<Corpus> list(Language language, File... roots) throws IOException {
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

    public static List<MultilingualCorpus> list(LanguagePair language, File... roots) throws IOException {
        ArrayList<MultilingualCorpus> output = new ArrayList<>();

        for (File directory : roots) {
            for (File file : FileUtils.listFiles(directory, TrueFileFilter.TRUE, FalseFileFilter.FALSE)) {
                String filename = file.getName();

                int lastDot = filename.lastIndexOf('.');
                if (lastDot < 0)
                    continue;

                String extension = filename.substring(lastDot + 1);
                filename = filename.substring(0, lastDot);

                if (filename.isEmpty() || extension.isEmpty())
                    continue;

                if (TMX_EXTENSION.equalsIgnoreCase(extension)) {
                    output.add(new TMXCorpus(filename, file));
                } else {
                    if (matchLang(language.source, extension)) {
                        File pair;

                        if (language.target.getRegion() == null) {
                            pair = new File(directory, filename + '.' + language.target.getLanguage());
                            if (!pair.isFile())
                                pair = new File(directory, filename + '.' + language.target.toLanguageTag());
                        } else {
                            pair = new File(directory, filename + '.' + language.target.toLanguageTag());
                        }

                        if (pair.isFile())
                            output.add(new ParallelFileCorpus(filename, language, file, pair));
                    }
                }
            }
        }

        return output;
    }

    private static boolean matchLang(Language language, String extension) {
        Language ext = Language.fromString(extension);
        if (language.getRegion() == null)
            return language.getLanguage().equals(ext.getLanguage());
        else
            return language.equals(ext);
    }

}
