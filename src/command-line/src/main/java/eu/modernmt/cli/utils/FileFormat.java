package eu.modernmt.cli.utils;

import eu.modernmt.lang.Language;
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

    MultilingualCorpus parse(Language source, Language target, File[] files) throws IllegalArgumentException;

    MultilingualCorpus rename(Language source, Language target, MultilingualCorpus corpus, File directory);

    class TMXFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(Language source, Language target, File[] files) throws IllegalArgumentException {
            if (files.length != 1)
                throw new IllegalArgumentException("Invalid number of arguments: expected 1 file");
            return new TMXCorpus(files[0]);
        }

        @Override
        public MultilingualCorpus rename(Language source, Language target, MultilingualCorpus corpus, File directory) {
            return new TMXCorpus(corpus.getName(), new File(directory, corpus.getName() + ".tmx"));
        }

    }

    class CompactFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(Language source, Language target, File[] files) throws IllegalArgumentException {
            if (files.length != 1)
                throw new IllegalArgumentException("Invalid number of arguments: expected 1 file");
            return new CompactFileCorpus(files[0]);
        }

        @Override
        public MultilingualCorpus rename(Language source, Language target, MultilingualCorpus corpus, File directory) {
            return new CompactFileCorpus(corpus.getName(), new File(directory, corpus.getName() + ".cq"));
        }

    }

    class ParallelFileFormat implements FileFormat {

        @Override
        public MultilingualCorpus parse(Language source, Language target, File[] files) throws IllegalArgumentException {
            if (files.length != 2)
                throw new IllegalArgumentException("Invalid number of arguments: expected 2 files");
            if (source == null)
                throw new IllegalArgumentException("Invalid input: source language is mandatory for parallel corpora");
            if (target == null)
                throw new IllegalArgumentException("Invalid input: target language is mandatory for parallel corpora");

            return new ParallelFileCorpus(new LanguagePair(source, target), files[0], files[1]);
        }

        @Override
        public MultilingualCorpus rename(Language source, Language target, MultilingualCorpus corpus, File directory) {
            return new ParallelFileCorpus(directory, corpus.getName(), new LanguagePair(source, target));
        }

    }

}
