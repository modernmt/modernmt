package eu.modernmt.model.corpus;

import eu.modernmt.io.FileProxy;
import eu.modernmt.io.LineReader;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
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
    public static final String CFC_EXTENSION = "cfc";

    public static MultilingualCorpus unwrap(MultilingualCorpus corpus) {
        while (corpus instanceof MultilingualCorpusWrapper) {
            corpus = ((MultilingualCorpusWrapper) corpus).getWrappedCorpus();
        }

        return corpus;
    }

    public static Corpus unwrap(Corpus corpus) {
        while (corpus instanceof CorpusWrapper) {
            corpus = ((CorpusWrapper) corpus).getWrappedCorpus();
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
            LanguageDirection language = ((ParallelFileCorpus) corpus).getLanguage();
            return new ParallelFileCorpus(folder, name, language);
        } else if (corpus instanceof CompactFileCorpus) {
            return new CompactFileCorpus(new File(folder, name + ".cfc"));
        } else {
            throw new IllegalArgumentException("Unknown multilingual corpus: " + corpus.getClass().getSimpleName());
        }
    }

    public static Corpus rename(Corpus corpus, File folder) {
        return rename(corpus, folder, corpus.getName());
    }

    public static Corpus rename(Corpus corpus, File folder, String name) {
        Language language = corpus.getLanguage();
        File file = new File(folder, name + '.' + language.toLanguageTag());
        return new FileCorpus(file, name, language);
    }

    public static long fileSize(MultilingualCorpus corpus) {
        corpus = unwrap(corpus);

        if (corpus instanceof TMXCorpus) {
            TMXCorpus tmxCorpus = (TMXCorpus) corpus;
            return fileSize(tmxCorpus.getFile());
        } else if (corpus instanceof ParallelFileCorpus) {
            ParallelFileCorpus parallelFileCorpus = ((ParallelFileCorpus) corpus);
            return fileSize(parallelFileCorpus.getSourceFile()) + fileSize(parallelFileCorpus.getTargetFile());
        } else if (corpus instanceof CompactFileCorpus) {
            CompactFileCorpus cfCorpus = (CompactFileCorpus) corpus;
            return fileSize(cfCorpus.getFile());
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

    public static long fileSize(Corpus corpus) {
        corpus = unwrap(corpus);

        if (corpus instanceof FileCorpus) {
            FileCorpus fileCorpus = (FileCorpus) corpus;
            return fileSize(fileCorpus.getFile());
        } else {
            throw new IllegalArgumentException("Unknown corpus: " + corpus.getClass().getSimpleName());
        }
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

    public static List<MultilingualCorpus> list(LanguageDirection language, File... roots) throws IOException {
        ArrayList<MultilingualCorpus> output = new ArrayList<>();

        HashMap<File, ParallelFileCorpusBuilder> builders = new HashMap<>();

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
                } else if (CFC_EXTENSION.equalsIgnoreCase(extension)) {
                    output.add(new CompactFileCorpus(filename, file));
                } else {
                    File key = new File(directory, filename);
                    ParallelFileCorpusBuilder builder = builders.computeIfAbsent(key, ParallelFileCorpusBuilder::new);
                    Language extLanguage = Language.fromString(extension);

                    if (language.source.isEqualOrMoreGenericThan(extLanguage))
                        builder.setSourceFile(file);
                    else if (language.target.isEqualOrMoreGenericThan(extLanguage))
                        builder.setTargetFile(file);
                }
            }
        }

        for (ParallelFileCorpusBuilder builder : builders.values()) {
            ParallelFileCorpus corpus = builder.getParallelFileCorpus(language);
            if (corpus != null)
                output.add(corpus);
        }

        return output;
    }

    private static class ParallelFileCorpusBuilder {

        public final String name;
        private File sourceFile = null;
        private File targetFile = null;

        public ParallelFileCorpusBuilder(File base) {
            this.name = base.getName();
        }

        public void setSourceFile(File sourceFile) throws IOException {
            if (this.sourceFile != null)
                throw new IOException("Duplicated entry file: " + sourceFile);
            this.sourceFile = sourceFile;
        }

        public void setTargetFile(File targetFile) throws IOException {
            if (this.targetFile != null)
                throw new IOException("Duplicated entry file: " + targetFile);
            this.targetFile = targetFile;
        }

        public ParallelFileCorpus getParallelFileCorpus(LanguageDirection language) {
            if (sourceFile == null || targetFile == null)
                return null;
            return new ParallelFileCorpus(name, language, sourceFile, targetFile);
        }
    }

}
