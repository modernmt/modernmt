package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.UTF8Charset;
import eu.modernmt.io.FileProxy;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.io.UnixLineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by davide on 31/07/17.
 */
public class CompactFileCorpus extends BaseMultilingualCorpus {

    private final String name;
    private final FileProxy file;

    public CompactFileCorpus(File file) {
        this(FilenameUtils.removeExtension(file.getName()), FileProxy.wrap(file));
    }

    public CompactFileCorpus(String name, File file) {
        this(name, FileProxy.wrap(file));
    }

    public CompactFileCorpus(FileProxy file) {
        this(FilenameUtils.removeExtension(file.getFilename()), file);
    }

    public CompactFileCorpus(String name, FileProxy file) {
        this.name = name;
        this.file = file;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        return new CompactLineReader(file);
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new CompactLineWriter(file, append);
    }

    private static class CompactLineReader implements MultilingualLineReader {

        private final UnixLineReader reader;
        private final HashMap<String, LanguagePair> cachedLanguagePairs = new HashMap<>();

        private CompactLineReader(FileProxy file) throws IOException {
            this.reader = new UnixLineReader(file.getInputStream(), UTF8Charset.get());
        }

        @Override
        public StringPair read() throws IOException {
            String source = reader.readLine();
            if (source == null)
                return null;

            String target = reader.readLine();
            if (target == null)
                throw new IOException("Missing target line at the end of CompactFileCorpus");

            String metadata = reader.readLine();
            if (metadata == null)
                throw new IOException("Missing metadata line at the end of CompactFileCorpus");

            return parse(metadata, source, target);
        }

        private StringPair parse(String metadata, String source, String target) throws IOException {
            String[] parts = metadata.split(",");
            if (parts.length != 2)
                throw new IOException("Invalid metadata found: " + metadata);

            Date timestamp = null;
            try {
                long date = Long.parseLong(parts[0]);
                if (date > 0)
                    timestamp = new Date(date);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid metadata found: " + metadata, e);
            }

            LanguagePair language = cachedLanguagePairs.computeIfAbsent(parts[1], key -> {
                String[] langs = key.split(" ");
                return new LanguagePair(Language.fromString(langs[0]), Language.fromString(langs[1]));
            });

            return new StringPair(language, source, target, timestamp);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

    private static class CompactLineWriter implements MultilingualLineWriter {

        private final UnixLineWriter writer;

        private CompactLineWriter(FileProxy file, boolean append) throws IOException {
            this.writer = new UnixLineWriter(file.getOutputStream(append), UTF8Charset.get());
        }

        @Override
        public void write(StringPair pair) throws IOException {
            writer.writeLine(pair.source);
            writer.writeLine(pair.target);
            writer.writeLine(encodeMetadata(pair));
        }

        private static String encodeMetadata(StringPair pair) {
            return (pair.timestamp == null ? "0" : Long.toString(pair.timestamp.getTime())) + ',' +
                    pair.language.source.toLanguageTag() + ' ' + pair.language.target.toLanguageTag();
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

    }

}
