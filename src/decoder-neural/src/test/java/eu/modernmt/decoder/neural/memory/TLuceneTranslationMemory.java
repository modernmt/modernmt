package eu.modernmt.decoder.neural.memory;

import eu.modernmt.data.DataBatch;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.*;

/**
 * Created by davide on 05/08/17.
 */
public class TLuceneTranslationMemory extends LuceneTranslationMemory {

    public TLuceneTranslationMemory(LanguagePair... languages) throws IOException {
        super(new LanguageIndex(Arrays.asList(languages)), new RAMDirectory(), 10);
    }

    public int size() throws IOException {
        return getIndexReader().numDocs();
    }

    public Set<Entry> entrySet() throws IOException {
        IndexReader reader = getIndexReader();
        int size = reader.numDocs();

        HashSet<Entry> result = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            Entry entry = Entry.parse(reader.document(i));

            if (entry != null)
                result.add(entry);
        }

        return result;
    }

    public static class Entry {

        private final long memory;
        private final LanguagePair language;
        private final String source;
        private final String target;

        private static Entry parse(Document doc) {
            long memory = Long.parseLong(doc.get("memory"));

            if (memory == 0L)
                return null;

            String[] langs = doc.get("language").split("__");

            LanguagePair language = new LanguagePair(Language.fromString(langs[0]), Language.fromString(langs[1]));
            String source = doc.get("content::" + langs[0]);
            String target = doc.get("content::" + langs[1]);

            return new Entry(memory, language, source, target);
        }

        public static Set<Entry> asEntrySet(LanguageIndex languages, Collection<TranslationUnit> units) {
            HashSet<Entry> result = new HashSet<>(units.size());

            for (TranslationUnit unit : units) {
                LanguagePair direction = languages.map(unit.direction);
                if (direction == null)
                    direction = unit.direction;

                String source = direction.source.toLanguageTag();
                String target = direction.target.toLanguageTag();

                Entry entry;
                if (source.compareTo(target) < 0)
                    entry = new Entry(unit.memory, direction, unit.rawSentence, unit.rawTranslation);
                else
                    entry = new Entry(unit.memory, direction.reversed(), unit.rawTranslation, unit.rawSentence);

                result.add(entry);
            }

            return result;
        }

        public Entry(long memory, LanguagePair language, String source, String target) {
            this.memory = memory;
            this.language = language;
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (memory != entry.memory) return false;
            if (!language.equals(entry.language)) return false;
            if (!source.equals(entry.source)) return false;
            return target.equals(entry.target);
        }

        @Override
        public int hashCode() {
            int result = (int) (memory ^ (memory >>> 32));
            result = 31 * result + language.hashCode();
            result = 31 * result + source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "memory=" + memory +
                    ", language=" + language +
                    ", source='" + source + '\'' +
                    ", target='" + target + '\'' +
                    '}';
        }
    }

    // DataListener utils

    public void onDelete(final Deletion deletion) throws IOException {
        super.OLDonDataReceived(new DataBatch() {

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

        super.OLDonDataReceived(new DataBatch() {
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
