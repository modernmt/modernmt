package eu.modernmt.contextanalyzer.lucene;

import eu.modernmt.contextanalyzer.lucene.analysis.CorpusContentField;
import eu.modernmt.model.corpus.Corpus;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;

import java.io.IOException;

/**
 * Created by davide on 23/09/15.
 */
public class DocumentBuilder {

    public static final String DOCUMENT_NAME_FIELD = "name";
    public static final String CONTENT_FIELD_PREFIX = "content:";

    public static String getContentField(Corpus corpus) {
        return CONTENT_FIELD_PREFIX + corpus.getLanguage();
    }

    public static String getLangOfContentField(String fieldName) throws IllegalArgumentException {
        if (fieldName.startsWith(CONTENT_FIELD_PREFIX)) {
            return fieldName.substring(CONTENT_FIELD_PREFIX.length());
        } else {
            throw new IllegalArgumentException("The field '" + fieldName + "' is not a valid content field name");
        }
    }

    public static Document createDocument(Corpus corpus) throws IOException {
        String fieldName = getContentField(corpus);

        Document doc = new Document();
        doc.add(new StringField(DOCUMENT_NAME_FIELD, corpus.getName(), Field.Store.YES));
        doc.add(new CorpusContentField(fieldName, corpus.getContentReader(), Field.Store.NO));

        return doc;
    }

}
