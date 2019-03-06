package eu.modernmt.backup.storage;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.DataListener;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.model.corpus.MultilingualCorpus;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CorporaBackupStorage implements DataListener, Closeable {

    private final WritersCache writers;
    private final CorporaChannels channels;
    private boolean closed = false;

    public CorporaBackupStorage(File path) throws IOException {
        this.writers = new WritersCache(new File(path, "corpora"), 512);
        this.channels = new CorporaChannels(new File(path, "channels"));
    }

    public synchronized void optimize() throws IOException {
        writers.optimize();
    }

    @Override
    public synchronized void onDataReceived(DataBatch batch) throws IOException {
        if (closed)
            return;

        // Store translation units

        MultilingualCorpus.MultilingualLineWriter writer;

        for (TranslationUnit unit : batch.getTranslationUnits()) {
            if (channels.skipData(unit.channel, unit.channelPosition))
                continue;

            writer = writers.getWriter(unit.memory, unit.direction);
            writer.write(new MultilingualCorpus.StringPair(unit.direction, unit.rawSentence, unit.rawTranslation, unit.timestamp));
        }

        writers.flush();

        for (Deletion deletion : batch.getDeletions()) {
            if (channels.skipData(deletion.channel, deletion.channelPosition))
                continue;

            writers.delete(deletion.memory);
        }

        // Update index and finalize

        channels.advanceChannels(batch.getChannelPositions());
    }

    @Override
    public Map<Short, Long> getLatestChannelPositions() {
        return channels.asMap();
    }

    @Override
    public boolean needsProcessing() {
        return false;
    }

    @Override
    public boolean needsAlignment() {
        return false;
    }

    @Override
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            writers.close();
        }
    }

}
