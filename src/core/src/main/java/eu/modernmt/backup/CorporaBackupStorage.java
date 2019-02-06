package eu.modernmt.backup;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import org.apache.commons.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CorporaBackupStorage implements DataListener, Closeable {

    private final File corporaPath;
    private final File channelsFile;
    private final Map<Short, Long> channels;
    private boolean closed = false;

    public CorporaBackupStorage(File path) throws IOException {
        this.corporaPath = new File(path, "corpora");
        this.channelsFile = new File(path, "channels");
        this.channels = parseChannels(this.channelsFile);

        FileUtils.forceMkdir(this.corporaPath);
    }

    private boolean skipData(short channel, long position) {
        Long existent = this.channels.get(channel);
        return existent != null && position <= existent;
    }

    private static Map<Short, Long> advanceChannels(Map<Short, Long> channels, Map<Short, Long> update) {
        channels = new HashMap<>(channels);

        for (Map.Entry<Short, Long> entry : update.entrySet()) {
            Short channel = entry.getKey();
            Long position = entry.getValue();
            Long existent = channels.get(channel);

            if (existent == null || position > existent)
                channels.put(channel, position);
        }

        return channels;
    }

    @Override
    public synchronized void onDataReceived(DataBatch batch) throws IOException {
        if (closed)
            return;

        CorporaCache cache = new CorporaCache();

        // Apply changes

        try {
            MultilingualCorpus.MultilingualLineWriter writer;

            for (TranslationUnit unit : batch.getTranslationUnits()) {
                if (skipData(unit.channel, unit.channelPosition))
                    continue;

                writer = cache.getWriter(unit.memory, unit.direction);
                writer.write(new MultilingualCorpus.StringPair(unit.direction, unit.rawSentence, unit.rawTranslation, unit.timestamp));
            }
        } finally {
            cache.close();
        }

        for (Deletion deletion : batch.getDeletions()) {
            if (skipData(deletion.channel, deletion.channelPosition))
                continue;

            cache.delete(deletion.memory);
        }

        // Update index and finalize

        Map<Short, Long> updatedChannels = advanceChannels(channels, batch.getChannelPositions());
        storeChannels(channelsFile, updatedChannels);
        channels.putAll(updatedChannels);
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return Collections.unmodifiableMap(channels);
    }

    @Override
    public boolean needsProcessing() {
        return false;
    }

    @Override
    public boolean needsAlignment() {
        return false;
    }

    // Channels file

    private static Map<Short, Long> parseChannels(File file) throws IOException {
        HashMap<Short, Long> channels = new HashMap<>();

        if (file.isFile()) {
            for (String line : FileUtils.readLines(file)) {
                String[] parts = line.split(" ", 2);
                channels.put(Short.parseShort(parts[0]), Long.parseLong(parts[1]));
            }
        }

        return channels;
    }

    private static void storeChannels(File file, Map<Short, Long> channels) throws IOException {
        ArrayList<String> lines = new ArrayList<>(channels.size());

        for (Map.Entry<Short, Long> entry : channels.entrySet()) {
            lines.add(Short.toString(entry.getKey()) + ' ' + entry.getValue());
        }

        FileUtils.writeLines(file, lines, false);
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    // Corpora cache

    private class CorporaCache implements Closeable {

        private final HashMap<File, MultilingualCorpus.MultilingualLineWriter> writers = new HashMap<>();

        public MultilingualCorpus.MultilingualLineWriter getWriter(long memory, LanguagePair direction) {
            String source = direction.source.getLanguage();
            String target = direction.target.getLanguage();

            if (source.compareTo(target) > 0) {
                String temp = source;
                source = target;
                target = temp;
            }

            File file = new File(corporaPath, "ModernMT_" + memory + "-" + source + "__" + target + ".cc");

            return writers.computeIfAbsent(file, f -> {
                try {
                    return new CompactFileCorpus(f).getContentWriter(true);
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            });
        }

        public void delete(long memory) {
            String prefix = "ModernMT_" + memory + "-";

            File[] files = corporaPath.listFiles((dir, name) -> name.startsWith(prefix));

            if (files != null) {
                for (File file : files)
                    file.delete();
            }
        }

        @Override
        public void close() throws IOException {
            for (MultilingualCorpus.MultilingualLineWriter writer : writers.values()) {
                writer.flush();
                writer.close();
            }
        }
    }
}
