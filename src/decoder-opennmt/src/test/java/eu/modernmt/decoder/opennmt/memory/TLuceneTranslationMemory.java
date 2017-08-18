package eu.modernmt.decoder.opennmt.memory;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.decoder.opennmt.memory.lucene.LuceneTranslationMemory;
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
        super(new LanguageIndex(Arrays.asList(languages)), new RAMDirectory());
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

        private final long domain;
        private final LanguagePair language;
        private final String source;
        private final String target;

        private static Entry parse(Document doc) {
            long domain = Long.parseLong(doc.get("domain"));

            if (domain == 0L)
                return null;

            String[] langs = doc.get("language").split("__");

            LanguagePair language = new LanguagePair(Locale.forLanguageTag(langs[0]), Locale.forLanguageTag(langs[1]));
            String source = doc.get("content::" + langs[0]);
            String target = doc.get("content::" + langs[1]);

            return new Entry(domain, language, source, target);
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
                    entry = new Entry(unit.domain, direction, unit.rawSourceSentence, unit.rawTargetSentence);
                else
                    entry = new Entry(unit.domain, direction.reversed(), unit.rawTargetSentence, unit.rawSourceSentence);

                result.add(entry);
            }

            return result;
        }

        public Entry(long domain, LanguagePair language, String source, String target) {
            this.domain = domain;
            this.language = language;
            this.source = source;
            this.target = target;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Entry entry = (Entry) o;

            if (domain != entry.domain) return false;
            if (!language.equals(entry.language)) return false;
            if (!source.equals(entry.source)) return false;
            return target.equals(entry.target);
        }

        @Override
        public int hashCode() {
            int result = (int) (domain ^ (domain >>> 32));
            result = 31 * result + language.hashCode();
            result = 31 * result + source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "domain=" + domain +
                    ", language=" + language +
                    ", source='" + source + '\'' +
                    ", target='" + target + '\'' +
                    '}';
        }
    }
}
