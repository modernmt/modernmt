package eu.modernmt.cli.utils;

import eu.modernmt.lang.LanguagePair;
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

    MultilingualCorpus parse(LanguagePair language, File file) throws IllegalArgumentException;

    MultilingualCorpus rename(LanguagePair language, MultilingualCorpus corpus, File directory);

    class TMXFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(LanguagePair language, File file) throws IllegalArgumentException {
            return new TMXCorpus(file);
        }

        @Override
        public MultilingualCorpus rename(LanguagePair language, MultilingualCorpus corpus, File directory) {
            return new TMXCorpus(corpus.getName(), new File(directory, corpus.getName() + ".tmx"));
        }

    }

    class CompactFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(LanguagePair language, File file) throws IllegalArgumentException {
            return new CompactFileCorpus(file);
        }

        @Override
        public MultilingualCorpus rename(LanguagePair language, MultilingualCorpus corpus, File directory) {
            return new CompactFileCorpus(corpus.getName(), new File(directory, corpus.getName() + ".cq"));
        }

    }

    class ParallelFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(LanguagePair language, File file) throws IllegalArgumentException {
            File parent = file.getParentFile();
            String name = file.getName();
            return new ParallelFileCorpus(parent, name, language);
        }

        @Override
        public MultilingualCorpus rename(LanguagePair language, MultilingualCorpus corpus, File directory) {
            return new ParallelFileCorpus(directory, corpus.getName(), language);
        }

    }

}
