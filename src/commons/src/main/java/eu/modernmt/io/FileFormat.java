package eu.modernmt.io;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;

import java.io.File;

public interface FileFormat {

    static FileFormat fromName(String name) {
        if ("tmx".equalsIgnoreCase(name)) {
            return new TMXFileFormat();
        } else if ("compact".equalsIgnoreCase(name)) {
            return new CompactFileFormat();
        } else if ("parallel".equalsIgnoreCase(name)) {
            return new ParallelFileFormat();
        } else {
            throw new IllegalArgumentException("Invalid file format: " + name);
        }
    }

    MultilingualCorpus parse(LanguageDirection language, File file) throws IllegalArgumentException;

    MultilingualCorpus rename(LanguageDirection language, MultilingualCorpus corpus, File directory);

    class TMXFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(LanguageDirection language, File file) throws IllegalArgumentException {
            return new TMXCorpus(file);
        }

        @Override
        public MultilingualCorpus rename(LanguageDirection language, MultilingualCorpus corpus, File directory) {
            FileStats stats = Corpora.stats(corpus);
            String extension = '.' + Corpora.TMX_EXTENSION;
            if (stats.gzipped) extension += ".gz";
            FileProxy file = new FileProxy.NativeFileProxy(new File(directory, corpus.getName() + extension), stats.gzipped);
            return new TMXCorpus(corpus.getName(), file);
        }

    }

    class CompactFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(LanguageDirection language, File file) throws IllegalArgumentException {
            return new CompactFileCorpus(file);
        }

        @Override
        public MultilingualCorpus rename(LanguageDirection language, MultilingualCorpus corpus, File directory) {
            FileStats stats = Corpora.stats(corpus);
            String extension = '.' + Corpora.COMPACT_EXTENSION;
            if (stats.gzipped) extension += ".gz";
            FileProxy file = new FileProxy.NativeFileProxy(new File(directory, corpus.getName() + extension), stats.gzipped);
            return new CompactFileCorpus(corpus.getName(), file);
        }

    }

    class ParallelFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(LanguageDirection language, File file) throws IllegalArgumentException {
            File parent = file.getParentFile();
            String name = file.getName();
            return new ParallelFileCorpus(parent, name, language);
        }

        @Override
        public MultilingualCorpus rename(LanguageDirection language, MultilingualCorpus corpus, File directory) {
            FileStats stats = Corpora.stats(corpus);
            String sourceExt = '.' + language.source.toLanguageTag();
            String targetExt = '.' + language.target.toLanguageTag();

            if (stats.gzipped) {
                sourceExt += ".gz";
                targetExt += ".gz";
            }

            FileProxy source = new FileProxy.NativeFileProxy(new File(directory, corpus.getName() + sourceExt), stats.gzipped);
            FileProxy target = new FileProxy.NativeFileProxy(new File(directory, corpus.getName() + targetExt), stats.gzipped);

            return new ParallelFileCorpus(corpus.getName(), language, source, target);
        }

    }

}
