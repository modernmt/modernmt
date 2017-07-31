package eu.modernmt.model.corpus;

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

    public static MultilingualCorpus rename(MultilingualCorpus corpus, File folder, String name) {
        if (corpus instanceof TMXCorpus) {
            return new TMXCorpus(new File(folder, name + ".tmx"));
        } else if (corpus instanceof ParallelFileCorpus) {
            LanguagePair language = ((ParallelFileCorpus) corpus).getLanguage();
            return new ParallelFileCorpus(folder, name, language);
        } else {
            throw new IllegalArgumentException("Unknown multilingual corpus: " + corpus.getClass().getSimpleName());
        }
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

    public static void list(Collection<Corpus> monolingualOutput, boolean monolingualIsTarget, Collection<MultilingualCorpus> bilingualOutput, Locale sourceLanguage, Locale targetLanguage, File... roots) throws IOException {
        for (File directory : roots) {
            HashMap<String, CorpusBuilder> builders = new HashMap<>();

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
                    builders.put(filename, new CorpusBuilder(filename, sourceLanguage, targetLanguage, file));
                } else {
                    Locale locale = Locale.forLanguageTag(extension);

                    if (sourceLanguage.getLanguage().equals(locale.getLanguage())) {
                        CorpusBuilder builder = builders.get(filename);

                        if (builder == null) {
                            builder = new CorpusBuilder(filename, sourceLanguage, targetLanguage);
                            builders.put(filename, builder);
                        }

                        builder.setSourceFile(file);
                    } else if (targetLanguage.getLanguage().equals(locale.getLanguage())) {
                        CorpusBuilder builder = builders.get(filename);

                        if (builder == null) {
                            builder = new CorpusBuilder(filename, sourceLanguage, targetLanguage);
                            builders.put(filename, builder);
                        }

                        builder.setTargetFile(file);
                    }
                }
            }

            for (CorpusBuilder builder : builders.values()) {
                if (builder.isMonolingual()) {
                    if (monolingualIsTarget && !builder.hasTargetFile())
                        continue;
                    if (!monolingualIsTarget && !builder.hasSourceFile())
                        continue;

                    if (monolingualOutput != null)
                        monolingualOutput.add(builder.buildMonolingual());
                } else {
                    if (bilingualOutput != null)
                        bilingualOutput.add(builder.buildBilingual());
                }
            }

        }

    }

    private static class CorpusBuilder {

        private final String name;
        private final LanguagePair language;

        private File sourceFile = null;
        private File targetFile = null;
        private final File tmxFile;

        CorpusBuilder(String name, Locale sourceLanguage, Locale targetLanguage) {
            this(name, sourceLanguage, targetLanguage, null);
        }

        CorpusBuilder(String name, Locale sourceLanguage, Locale targetLanguage, File tmxFile) {
            this.language = new LanguagePair(sourceLanguage, targetLanguage);
            this.tmxFile = tmxFile;
            this.name = name;
        }

        void setSourceFile(File sourceFile) {
            this.sourceFile = sourceFile;
        }

        void setTargetFile(File targetFile) {
            this.targetFile = targetFile;
        }

        boolean isMonolingual() {
            return tmxFile == null && ((sourceFile == null && targetFile != null) || (sourceFile != null && targetFile == null));
        }

        boolean hasSourceFile() {
            return sourceFile != null;
        }

        boolean hasTargetFile() {
            return targetFile != null;
        }

        MultilingualCorpus buildBilingual() {
            if (tmxFile == null) {
                return new ParallelFileCorpus(this.name, this.language, this.sourceFile, this.targetFile);
            } else {
                return new TMXCorpus(this.name, this.tmxFile);
            }
        }

        Corpus buildMonolingual() {
            if (sourceFile != null)
                return new FileCorpus(this.sourceFile, this.name, this.language.source);
            else
                return new FileCorpus(this.targetFile, this.name, this.language.target);
        }
    }

}
