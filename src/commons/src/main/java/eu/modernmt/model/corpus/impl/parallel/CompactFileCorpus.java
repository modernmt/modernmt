package eu.modernmt.model.corpus.impl.parallel;

import eu.modernmt.io.UTF8Charset;
import eu.modernmt.io.FileProxy;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.io.UnixLineWriter;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.BaseMultilingualCorpus;
import eu.modernmt.model.corpus.TUReader;
import eu.modernmt.model.corpus.TUWriter;
import eu.modernmt.model.corpus.TranslationUnit;
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

    public FileProxy getFile() {
        return file;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TUReader getContentReader() throws IOException {
        return new CompactReader(file);
    }

    @Override
    public TUWriter getContentWriter(boolean append) throws IOException {
        return new CompactWriter(file, append);
    }

    private static class CompactReader implements TUReader {

        private final UnixLineReader reader;
        private final HashMap<String, LanguageDirection> cachedLanguagePairs = new HashMap<>();

        private CompactReader(FileProxy file) throws IOException {
            this.reader = new UnixLineReader(file.getInputStream(), UTF8Charset.get());
        }

        @Override
        public TranslationUnit read() throws IOException {
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

        private TranslationUnit parse(String metadata, String source, String target) throws IOException {
            String[] parts = metadata.split(",", 3);
            if (parts.length < 2)
                throw new IOException("Invalid metadata found: " + metadata);

            Date timestamp = null;
            try {
                long date = Long.parseLong(parts[0]);
                if (date > 0)
                    timestamp = new Date(date);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid metadata found: " + metadata, e);
            }

            LanguageDirection language = cachedLanguagePairs.computeIfAbsent(parts[1], key -> {
                String[] langs = key.split(" ");
                return new LanguageDirection(Language.fromString(langs[0]), Language.fromString(langs[1]));
            });

            String tuid = parts.length > 2 ? parts[2] : null;

            return new TranslationUnit(tuid, language, source, target, timestamp);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

    private static class CompactWriter implements TUWriter {

        private final UnixLineWriter writer;

        private CompactWriter(FileProxy file, boolean append) throws IOException {
            this.writer = new UnixLineWriter(file.getOutputStream(append), UTF8Charset.get());
        }

        @Override
        public void write(TranslationUnit tu) throws IOException {
            writer.writeLine(tu.source);
            writer.writeLine(tu.target);
            writer.writeLine(encodeMetadata(tu));
        }

        private static String encodeMetadata(TranslationUnit tu) {
            StringBuilder metadata = new StringBuilder();
            metadata.append(tu.timestamp == null ? "0" : Long.toString(tu.timestamp.getTime()));
            metadata.append(',');
            metadata.append(tu.language.source.toLanguageTag())
                    .append(' ')
                    .append(tu.language.target.toLanguageTag());
            if (tu.tuid != null) {
                metadata.append(',');
                metadata.append(tu.tuid);
            }

            return metadata.toString();
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
