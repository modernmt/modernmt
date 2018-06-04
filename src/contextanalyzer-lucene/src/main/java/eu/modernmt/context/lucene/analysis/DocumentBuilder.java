package eu.modernmt.context.lucene.analysis;

import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpus;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

import java.io.IOException;
import java.io.Reader;

/**
 * Created by davide on 23/09/15.
 */
public class DocumentBuilder {

    // Factory methods

    public static Document newInstance(LanguagePair direction, Corpus corpus) throws IOException {
        return newInstance(0L, direction, corpus);
    }

    public static Document newInstance(long memory, LanguagePair direction, Corpus corpus) throws IOException {
        return newInstance(memory, direction, corpus.getRawContentReader());
    }

    public static Document updatedInstance(String docId, Reader contentReader) {
        String[] parts = docId.split("_");
        if (parts.length != 3)
            throw new IllegalArgumentException("Invalid Document ID: " + docId);

        long memory = Long.parseLong(parts[0]);
        LanguagePair direction = new LanguagePair(new Language(parts[1]), new Language(parts[2]));

        return newInstance(memory, direction, contentReader);
    }

    public static Document newInstance(long memory, LanguagePair direction, Reader contentReader) {
        Document document = new Document();
        document.add(new StringField(DOC_ID_FIELD, makeId(memory, direction), Field.Store.NO));
        document.add(new LongField(MEMORY_FIELD, memory, Field.Store.YES));
        document.add(new CorpusContentField(makeContentFieldName(direction), contentReader));

        return document;
    }

    private static final String DOC_ID_FIELD = "cid";
    private static final String MEMORY_FIELD = "memory";
    private static final String CONTENT_PREFIX_FIELD = "content_";

    // Getters

    public static String getId(Document self) {
        return self.get(DOC_ID_FIELD);
    }

    public static long getMemory(Document self) {
        return Long.parseLong(self.get(MEMORY_FIELD));
    }

    public static long getMemory(String documentId) {
        return Long.parseLong(documentId.substring(0, documentId.indexOf('_')));
    }

    public static String getLanguageForContentField(String field) {
        if (!field.startsWith(CONTENT_PREFIX_FIELD))
            return null;

        return field.substring(CONTENT_PREFIX_FIELD.length(), field.lastIndexOf('_'));
    }

    // Term constructors

    public static Term makeIdTerm(String id) {
        return new Term(DocumentBuilder.DOC_ID_FIELD, id);
    }

    public static Term makeMemoryTerm(long memory) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(memory, 0, builder);

        return new Term(MEMORY_FIELD, builder.toBytesRef());
    }

    // Value builders

    public static String makeId(long memory, LanguagePair direction) {
        return Long.toString(memory) + '_' + direction.source.getLanguage() + '_' + direction.target.getLanguage();
    }

    public static String makeContentFieldName(LanguagePair direction) {
        return CONTENT_PREFIX_FIELD + direction.source.getLanguage() + '_' + direction.target.getLanguage();
    }

}
