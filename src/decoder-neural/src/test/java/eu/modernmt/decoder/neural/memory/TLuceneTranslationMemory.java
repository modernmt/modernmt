package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.memory.ScoreEntry;
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

    @Override
    public int size() {
        try {
            return getIndexReader().numDocs();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public Set<ScoreEntry> entrySet() throws IOException {
        HashSet<ScoreEntry> result = new HashSet<>();
        super.dump(result::add);
        return result;
    }

    public static Set<ScoreEntry> asEntrySet(Collection<TranslationUnit> units) {
        HashSet<ScoreEntry> result = new HashSet<>(units.size());

        for (TranslationUnit unit : units) {
            String source = unit.language.source.toLanguageTag();
            String target = unit.language.target.toLanguageTag();

            ScoreEntry entry;
            if (source.compareTo(target) < 0)
                entry = new ScoreEntry(unit.memory, unit.language,
                        unit.rawSentence.split("\\s+"), unit.rawTranslation.split("\\s+"));
            else
                entry = new ScoreEntry(unit.memory, unit.language.reversed(),
                        unit.rawTranslation.split("\\s+"), unit.rawSentence.split("\\s+"));

            result.add(entry);
        }

        return result;
    }

    // DataListener utils

    public void onDelete(final Deletion deletion) throws IOException {
        super.onDataReceived(new DataBatch() {

            @Override
            public Collection<TranslationUnit> getDiscardedTranslationUnits() {
                return new ArrayList<>();
            }

            @Override
            public Collection<TranslationUnit> getTranslationUnits() {
                return Collections.emptyList();
            }

            @Override
            public Collection<Deletion> getDeletions() {
                return Collections.singleton(deletion);
            }

            @Override
            public Map<Short, Long> getChannelPositions() {
                return Collections.singletonMap(deletion.channel, deletion.channelPosition);
            }

        });
    }

    public void onDataReceived(Collection<TranslationUnit> units) throws IOException {
        final HashMap<Short, Long> positions = new HashMap<>();
        for (TranslationUnit unit : units) {
            Long existingPosition = positions.get(unit.channel);

            if (existingPosition == null || existingPosition < unit.channelPosition)
                positions.put(unit.channel, unit.channelPosition);
        }

        super.onDataReceived(new DataBatch() {
            @Override
            public Collection<TranslationUnit> getDiscardedTranslationUnits() {
                return new ArrayList<>();
            }

            @Override
            public Collection<TranslationUnit> getTranslationUnits() {
                return units;
            }

            @Override
            public Collection<Deletion> getDeletions() {
                return Collections.emptyList();
            }

            @Override
            public Map<Short, Long> getChannelPositions() {
                return positions;
            }
        });
    }
}
