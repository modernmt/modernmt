package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.DeletionMessage;
import eu.modernmt.data.TranslationUnitMessage;
import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.decoder.neural.memory.lucene.query.QueryBuilder;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.memory.TranslationMemory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 05/08/17.
 */
public class TLuceneTranslationMemory extends LuceneTranslationMemory {

    public TLuceneTranslationMemory() throws IOException {
        super(new RAMDirectory(), 10);
    }

    public DocumentBuilder getDocumentBuilder() {
        return super.documentBuilder;
    }

    public QueryBuilder getQueryBuilder() {
        return super.queryBuilder;
    }

    @Override
    public int size() {
        try {
            return getIndexReader().numDocs();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public Set<TranslationMemory.Entry> entrySet() throws IOException {
        HashSet<TranslationMemory.Entry> result = new HashSet<>();
        super.dumpAll(result::add);
        return result;
    }

    public static Set<TranslationMemory.Entry> asEntrySet(TranslationUnitMessage... units) {
        return asEntrySet(Arrays.asList(units));
    }

    public static Set<TranslationMemory.Entry> asEntrySet(Collection<TranslationUnitMessage> units) {
        HashSet<TranslationMemory.Entry> result = new HashSet<>(units.size());

        for (TranslationUnitMessage unit : units) {
            //TODO: Memory.Entry should have "tuid" filed
            TranslationMemory.Entry entry = new TranslationMemory.Entry(unit.memory, unit.language, unit.value.source, unit.value.target);
            result.add(entry);
        }

        return result;
    }

    // DataListener utils

    public void onDelete(final DeletionMessage deletion) throws IOException {
        super.onDataReceived(new DataBatch() {

            @Override
            public Collection<TranslationUnitMessage> getTranslationUnits() {
                return Collections.emptyList();
            }

            @Override
            public Collection<DeletionMessage> getDeletions() {
                return Collections.singleton(deletion);
            }

            @Override
            public Map<Short, Long> getChannelPositions() {
                return Collections.singletonMap(deletion.channel, deletion.channelPosition);
            }

        });
    }

    public void onDataReceived(TranslationUnitMessage... units) throws IOException {
        onDataReceived(Arrays.asList(units));
    }

    public void onDataReceived(TranslationUnitMessage unit) throws IOException {
        onDataReceived(Collections.singleton(unit));
    }

    public void onDataReceived(Collection<TranslationUnitMessage> units) throws IOException {
        final HashMap<Short, Long> positions = new HashMap<>();
        for (TranslationUnitMessage unit : units) {
            Long existingPosition = positions.get(unit.channel);

            if (existingPosition == null || existingPosition < unit.channelPosition)
                positions.put(unit.channel, unit.channelPosition);
        }

        super.onDataReceived(new DataBatch() {

            @Override
            public Collection<TranslationUnitMessage> getTranslationUnits() {
                return units;
            }

            @Override
            public Collection<DeletionMessage> getDeletions() {
                return Collections.emptyList();
            }

            @Override
            public Map<Short, Long> getChannelPositions() {
                return positions;
            }
        });
    }
}
