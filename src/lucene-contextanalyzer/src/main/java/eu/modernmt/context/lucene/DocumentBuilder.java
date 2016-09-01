package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.CorpusContentField;
import eu.modernmt.model.corpus.Corpus;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 23/09/15.
 */
public class DocumentBuilder {

    public static final String DOCUMENT_NAME_FIELD = "name";
    public static final String CONTENT_FIELD = "content";

    public static Document createDocument(Corpus document) throws ContextAnalyzerException {
        String name = document.getName();

        if (name == null)
            name = "UNKNOWN";

        Document doc = new Document();
        doc.add(new StringField(DOCUMENT_NAME_FIELD, name, Field.Store.YES));

        try {
            doc.add(new CorpusContentField(CONTENT_FIELD, document.getRawContentReader(), Field.Store.NO));
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to build document " + document.getName(), e);
        }

        return doc;
    }

}
