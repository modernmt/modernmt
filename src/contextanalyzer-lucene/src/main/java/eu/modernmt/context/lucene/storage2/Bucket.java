package eu.modernmt.context.lucene.storage2;

import eu.modernmt.lang.LanguagePair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.*;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

class Bucket {

    private final long id;
    private final LanguagePair language;
    private final UUID owner;

    final File path;
    final File gzPath;

    long plainTextFileSize;
    long compressedFileSize;
    long virtualSize; // size of file including uncompressed bytes size of compressed content

    private BucketWriter writer = null;

    Bucket(File folder, long id, LanguagePair language, UUID owner) {
        this.id = id;
        this.language = language;
        this.owner = owner;

        String key = Long.toString(id) + '_' + language.source.getLanguage() + '_' + language.target.getLanguage();
        this.path = new File(folder, key);
        this.gzPath = new File(folder, key + ".gz");

        this.plainTextFileSize = 0;
        this.compressedFileSize = 0;
        this.virtualSize = 0;
    }

    Bucket(File folder, long id, LanguagePair language, UUID owner, long plainTextFileSize, long compressedFileSize, long virtualSize) {
        this(folder, id, language, owner);

        this.plainTextFileSize = plainTextFileSize;
        this.compressedFileSize = compressedFileSize;
        this.virtualSize = virtualSize;
    }

    public long getId() {
        return id;
    }

    public LanguagePair getLanguage() {
        return language;
    }

    public UUID getOwner() {
        return owner;
    }

    public long getSize() {
        return virtualSize;
    }

    BucketWriter getWriter() {
        if (writer == null) {
            synchronized (this) {
                if (writer == null)
                    writer = new BucketWriter(this);
            }
        }

        return writer;
    }

    public InputStream getContentStream() throws IOException {
        boolean success = false;

        InputStream gzStream = null;
        InputStream stream = null;

        try {
            synchronized (this) {
                if (gzPath.exists() && compressedFileSize > 0)
                    gzStream = new GZIPInputStream(new BoundedInputStream(new FileInputStream(gzPath), compressedFileSize));

                if (path.exists() && plainTextFileSize > 0)
                    stream = new BoundedInputStream(new FileInputStream(path), plainTextFileSize);
            }

            success = true;

            if (gzStream != null && stream != null)
                return new SequenceInputStream(gzStream, stream);
            else if (gzStream != null)
                return gzStream;
            else if (stream != null)
                return stream;
            else
                return new InputStream() {
                    @Override
                    public int read() {
                        return -1;
                    }
                };
        } finally {
            if (!success)
                IOUtils.closeQuietly(gzStream);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bucket bucket = (Bucket) o;

        if (id != bucket.id) return false;
        return language.equals(bucket.language);
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + language.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Bucket{" + id + ", " + language + '}';
    }

}
