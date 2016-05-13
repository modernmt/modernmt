package eu.modernmt.context.lucene;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.lucene.analysis.CorpusContentField;
import eu.modernmt.model.Corpus;
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
    public static final String CONTENT_FIELD_PREFIX = "content:";

    public static String getContentField(Corpus document) {
        return CONTENT_FIELD_PREFIX + document.getLanguage().toLanguageTag();
    }

    public static Locale getLangOfContentField(String fieldName) throws IllegalArgumentException {
        if (fieldName.startsWith(CONTENT_FIELD_PREFIX)) {
            return Locale.forLanguageTag(fieldName.substring(CONTENT_FIELD_PREFIX.length()));
        } else {
            throw new IllegalArgumentException("The field '" + fieldName + "' is not a valid content field name");
        }
    }

    public static Document createDocument(Corpus document) throws ContextAnalyzerException {
        String fieldName = getContentField(document);
        String name = document.getName();

        if (name == null)
            name = "UNKNOWN";

        Document doc = new Document();
        doc.add(new StringField(DOCUMENT_NAME_FIELD, name, Field.Store.YES));

        try {
            doc.add(new CorpusContentField(fieldName, document.getContentReader(), Field.Store.NO));
        } catch (IOException e) {
            throw new ContextAnalyzerException("Unable to build document " + document.getName(), e);
        }

        return doc;
    }

}
