package eu.modernmt.context.lucene;

import eu.modernmt.context.lucene.analysis.CorpusContentField;
import eu.modernmt.model.corpus.Corpus;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 23/09/15.
 */
public class DocumentBuilder {

    public static final String ID_FIELD = "id";
    public static final String CONTENT_FIELD = "content";

    public static long getId(Document document) {
        return Long.parseLong(document.get(ID_FIELD));
    }

    public static Document createDocument(Corpus corpus) throws IOException {
        return createDocument(0L, corpus);
    }

    public static Document createDocument(long domainId, Corpus corpus) throws IOException {
        Reader reader = corpus.getRawContentReader();
        return createDocument(domainId, reader);
    }

    public static Document createDocument(long domainId, Reader contentReader) {
        Document document = new Document();
        document.add(new LongField(ID_FIELD, domainId, Field.Store.YES));
        document.add(new CorpusContentField(CONTENT_FIELD, contentReader, Field.Store.NO));

        return document;
    }

}
