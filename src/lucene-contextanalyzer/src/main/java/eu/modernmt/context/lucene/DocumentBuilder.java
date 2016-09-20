package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.CorpusContentField;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.Corpus;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;

import java.io.IOException;

/**
 * Created by davide on 23/09/15.
 */
public class DocumentBuilder {

    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String CONTENT_FIELD = "content";

    public static int getId(Document document) {
        return Integer.parseInt(document.get(ID_FIELD));
    }

    public static String getName(Document document) {
        return document.get(NAME_FIELD);
    }

    public static Document createDocument(Corpus corpus) throws ContextAnalyzerException {
        return createDocument(null, corpus);
    }

    public static Document createDocument(Domain domain, Corpus corpus) throws ContextAnalyzerException {
        int id = 0;
        String name = "null";

        if (domain != null) {
            id = domain.getId();
            name = domain.getName();
        }

        Document document = new Document();
        document.add(new IntField(ID_FIELD, id, Field.Store.YES));
        document.add(new StringField(NAME_FIELD, name, Field.Store.YES));

        try {
            document.add(new CorpusContentField(CONTENT_FIELD, corpus.getRawContentReader(), Field.Store.NO));
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to build document " + name, e);
        }

        return document;
    }

}
