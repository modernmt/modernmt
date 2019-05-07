package eu.modernmt.context.lucene.storage.utils;

import eu.modernmt.context.lucene.storage.Bucket;
import eu.modernmt.context.lucene.storage.CorporaStorage;
import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.io.UnixLineReader;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

public class Dump {

    private static final class DumpableCorporaStorage extends CorporaStorage {

        public DumpableCorporaStorage(File path) throws IOException {
            super(path);
        }

        public void forEach(Consumer<Bucket> consumer) throws IOException {
            for (Bucket bucket : super.buckets.getAll())
                consumer.accept(bucket);
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length != 1)
            throw new IllegalArgumentException("Wrong number of arguments, usage: <model-path>");

        DumpableCorporaStorage storage = new DumpableCorporaStorage(new File(args[0]));
        storage.forEach(bucket -> {
            try {
                dump(bucket);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        });
    }

    private static void dump(Bucket bucket) throws IOException {
        String prefix = bucket.getId() + "\t" + bucket.getLanguage().source + "\t" + bucket.getLanguage().target + "\t";

        UnixLineReader reader = null;

        try {
            reader = new UnixLineReader(bucket.getContentStream(), UTF8Charset.get());

            String line;
            while ((line = reader.readLine()) != null)
                System.out.println(prefix + line);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
