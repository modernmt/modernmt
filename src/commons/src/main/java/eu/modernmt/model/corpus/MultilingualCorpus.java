package eu.modernmt.model.corpus;

import eu.modernmt.lang.LanguagePair;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

/**
 * Created by davide on 24/02/16.
 */
public interface MultilingualCorpus {

    String getName();

    Set<LanguagePair> getLanguages();

    int getLineCount(LanguagePair language);

    MultilingualLineReader getContentReader() throws IOException;

    MultilingualLineWriter getContentWriter(boolean append) throws IOException;

    Corpus getCorpus(LanguagePair language, boolean source);

    interface MultilingualLineReader extends Closeable {

        StringPair read() throws IOException;

    }

    interface MultilingualLineWriter extends Closeable {

        void write(StringPair pair) throws IOException;

        void flush() throws IOException;
    }

    class StringPair {

        public LanguagePair language;
        public String source;
        public String target;
        public Date timestamp;

        public StringPair(LanguagePair language, String source, String target) {
            this(language, source, target, null);
        }

        public StringPair(LanguagePair language, String source, String target, Date timestamp) {
            this.language = language;
            this.source = source;
            this.target = target;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StringPair that = (StringPair) o;

            if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) return false;
            if (!language.equals(that.language)) return false;
            if (!source.equals(that.source)) return false;
            return target.equals(that.target);
        }

        @Override
        public int hashCode() {
            int result = timestamp != null ? timestamp.hashCode() : 0;
            result = 31 * result + language.hashCode();
            result = 31 * result + source.hashCode();
            result = 31 * result + target.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return '[' + language.toString() + "]<" + source + ',' + target + '>';
        }
    }
}
