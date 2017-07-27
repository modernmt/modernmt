package eu.modernmt.context.lucene.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo;

import java.io.Reader;

public class CorpusContentField extends Field {

    public static final FieldType TYPE_NOT_STORED = new FieldType();

    static {
        TYPE_NOT_STORED.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS);
        TYPE_NOT_STORED.setIndexed(true);
        TYPE_NOT_STORED.setTokenized(true);
        TYPE_NOT_STORED.setStoreTermVectors(true);
        TYPE_NOT_STORED.freeze();
    }

    public CorpusContentField(String name, Reader reader) {
        super(name, reader, TYPE_NOT_STORED);
    }

    public CorpusContentField(String name, String value) {
        super(name, value, TYPE_NOT_STORED);
    }

    public CorpusContentField(String name, TokenStream stream) {
        super(name, stream, TYPE_NOT_STORED);
    }

}
