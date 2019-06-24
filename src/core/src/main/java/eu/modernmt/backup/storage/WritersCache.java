package eu.modernmt.backup.storage;

import eu.modernmt.io.Paths;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

class WritersCache implements Closeable {

    private final File root;
    private final LinkedHashMap<File, MultilingualCorpus.MultilingualLineWriter> writers;

    WritersCache(File root, int capacity) {
        this.root = root;
        this.writers = new LinkedHashMap<File, MultilingualCorpus.MultilingualLineWriter>(capacity + 1, 1.f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<File, MultilingualCorpus.MultilingualLineWriter> eldest) {
                boolean remove = size() > capacity;

                if (remove) {
                    MultilingualCorpus.MultilingualLineWriter writer = eldest.getValue();
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeIOException(e);
                    }
                }

                return remove;
            }

        };
    }

    private File getMemoryFolder(long memory) {
        return Paths.join(root, Long.toString(memory % 1000), Long.toString(memory));
    }

    public MultilingualCorpus.MultilingualLineWriter getWriter(long memory, LanguageDirection direction) {
        String source = direction.source.getLanguage();
        String target = direction.target.getLanguage();

        if (source.compareTo(target) > 0) {
            String temp = source;
            source = target;
            target = temp;
        }

        File folder = getMemoryFolder(memory);
        File file = new File(folder, source + "__" + target + ".cfc");

        return writers.computeIfAbsent(file, f -> {
            try {
                FileUtils.forceMkdir(folder);
                return new CompactFileCorpus(f).getContentWriter(true);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        });
    }

    public void delete(long memory) throws IOException {
        File folder = getMemoryFolder(memory);

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".cfc"));
        if (files != null) {
            for (File file : files) {
                MultilingualCorpus.MultilingualLineWriter writer = writers.remove(file);

                if (writer != null)
                    writer.close();
            }
        }

        if (folder.exists())
            FileUtils.forceDelete(folder);
    }

    public void flush() throws IOException {
        for (MultilingualCorpus.MultilingualLineWriter writer : writers.values())
            writer.flush();
    }

    @Override
    public void close() throws IOException {
        flush();

        for (MultilingualCorpus.MultilingualLineWriter writer : writers.values())
            writer.close();

        writers.clear();
    }

    public void optimize() throws IOException {
        close();

        for (File cfcFile : FileUtils.listFiles(root, new String[] {"cfc"}, true)) {
            File gzFile = new File(cfcFile.getParent(), cfcFile.getName() + ".gz");
            File existingGzFile = new File(cfcFile.getParent(), cfcFile.getName() + ".gz_");

            if (gzFile.exists())
                FileUtils.moveFile(gzFile, existingGzFile);

            copy(existingGzFile, cfcFile, gzFile);

            FileUtils.deleteQuietly(existingGzFile);
            FileUtils.deleteQuietly(cfcFile);
        }
    }

    public void copy(File gzFile, File cfcFile, File outputFile) throws IOException {
        OutputStream output = null;
        InputStream gzInput = null;
        InputStream cfcInput = null;

        try {
            output = new GZIPOutputStream(new FileOutputStream(outputFile, false));

            if (gzFile.isFile()) {
                gzInput = new GZIPInputStream(new FileInputStream(gzFile));
                IOUtils.copy(gzInput, output);
            }

            cfcInput = new FileInputStream(cfcFile);
            IOUtils.copy(cfcInput, output);
        } finally {
            IOUtils.closeQuietly(cfcInput);
            IOUtils.closeQuietly(gzInput);
            IOUtils.closeQuietly(output);
        }
    }
}
