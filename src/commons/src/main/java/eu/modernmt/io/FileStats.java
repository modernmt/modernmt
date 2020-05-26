package eu.modernmt.io;

import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;

import java.io.File;

public class FileStats {

    public enum Type {
        TMX, PARALLEL, COMPACT, FILE
    }

    public final Type type;
    public final String name;
    public final String extension;
    public final boolean gzipped;
    public final long size;

    static FileStats of(Corpus corpus) {
        if (!(corpus instanceof FileCorpus))
            throw new IllegalArgumentException("Unknown corpus type: " + corpus.getClass().getSimpleName());
        return of(((FileCorpus) corpus).getFile());
    }

    static FileStats of(FileProxy file) {
        if (!(file instanceof FileProxy.NativeFileProxy))
            throw new IllegalArgumentException("Unknown file proxy type: " + file.getClass().getSimpleName());
        return of(((FileProxy.NativeFileProxy) file).getFile());
    }

    static FileStats of(File file) {
        long size = file.length();
        boolean gzip = false;

        String[] filename = getFilename(file);
        if ("gz".equalsIgnoreCase(filename[1])) {
            gzip = true;
            filename = getFilename(filename[0]);
        }

        Type type;
        if (Corpora.TMX_EXTENSION.equalsIgnoreCase(filename[1])) {
            type = Type.TMX;
            if (gzip) size *= 5.5;
        } else if (Corpora.COMPACT_EXTENSION.equalsIgnoreCase(filename[1])) {
            type = Type.COMPACT;
            if (gzip) size *= 3;
        } else {
            type = Type.FILE;
            if (gzip) size *= 3;
        }

        return new FileStats(type, filename[0], filename[1], gzip, size);
    }

    static FileStats of(MultilingualCorpus corpus) {
        if (corpus instanceof TMXCorpus) {
            return of(((TMXCorpus) corpus).getFile());
        } else if (corpus instanceof ParallelFileCorpus) {
            FileStats source = of(((ParallelFileCorpus) corpus).getSourceFile());
            FileStats target = of(((ParallelFileCorpus) corpus).getTargetFile());
            return new FileStats(Type.PARALLEL, source.name, null, source.gzipped, source.size + target.size);
        } else if (corpus instanceof CompactFileCorpus) {
            return of(((CompactFileCorpus) corpus).getFile());
        } else {
            throw new IllegalArgumentException("Unknown multilingual corpus: " + corpus.getClass().getSimpleName());
        }
    }

    private static String[] getFilename(File file) {
        return getFilename(file.getName());
    }

    private static String[] getFilename(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return new String[]{filename, ""};
        return new String[]{filename.substring(0, dot), filename.substring(dot + 1)};
    }

    private FileStats(Type type, String name, String extension, boolean gzipped, long size) {
        this.type = type;
        this.name = name;
        this.extension = extension;
        this.gzipped = gzipped;
        this.size = size;
    }
}
