package eu.modernmt.model.corpus;

import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

/**
 * Created by davide on 24/02/16.
 */
public interface BilingualCorpus {

    static int getLineCount(BilingualCorpus corpus) throws IOException {
        BilingualLineReader reader = null;

        try {
            int count = 0;
            reader = corpus.getContentReader();

            while (reader.read() != null)
                count++;

            return count;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    String getName();

    Locale getSourceLanguage();

    Locale getTargetLanguage();

    int getLineCount() throws IOException;

    BilingualLineReader getContentReader() throws IOException;

    BilingualLineWriter getContentWriter(boolean append) throws IOException;

    Corpus getSourceCorpus();

    Corpus getTargetCorpus();

    interface BilingualLineReader extends Closeable {

        StringPair read() throws IOException;

    }

    interface BilingualLineWriter extends Closeable {

        void write(String source, String target) throws IOException;

        void write(StringPair pair) throws IOException;

    }

    class StringPair {

        /**
         * If true, the pair metadata indicate that the original
         * source and target languages were inverted.
         */
        public boolean inverted = false;

        public String source;
        public String target;
        public Date timestamp;

        public StringPair(String source, String target) {
            this.source = source;
            this.target = target;
            this.timestamp = null;
        }

        public StringPair(String source, String target, Date timestamp) {
            this.source = source;
            this.target = target;
            this.timestamp = timestamp;
        }

        public StringPair(String source, String target, Date timestamp, boolean inverted) {
            this.inverted = inverted;
            this.source = source;
            this.target = target;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StringPair that = (StringPair) o;

            if (!source.equals(that.source)) return false;
            if (!target.equals(that.target)) return false;
            return timestamp != null ? timestamp.equals(that.timestamp) : that.timestamp == null;

        }

        @Override
        public int hashCode() {
            int result = source.hashCode();
            result = 31 * result + target.hashCode();
            result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
            return result;
        }
    }
}
