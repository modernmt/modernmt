package eu.modernmt.context.lucene.analysis;

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
import java.util.Locale;

/**
 * Created by davide on 23/09/15.
 */
public class DocumentBuilder {

    public static final String DOCID_FIELD = "docid";
    private static final String MEMORY_FIELD = "memory";
    private static final String LANGUAGE_FIELD = "language";
    private static final String CONTENT_PREFIX_FIELD = "content__";

    // Fields

    public static String getContentFieldName(LanguagePair direction) {
        return CONTENT_PREFIX_FIELD + direction.source.toLanguageTag();
    }

    // Terms

    public static Term makeLanguageTerm(LanguagePair direction) {
        return new Term(LANGUAGE_FIELD, makeLanguageTag(direction));
    }

    public static Term makeMemoryTerm(long memory) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(memory, 0, builder);

        return new Term(MEMORY_FIELD, builder.toBytesRef());
    }

    public static Term makeDocumentIdTerm(long memory, LanguagePair direction) {
        return new Term(DOCID_FIELD, makeDocumentId(direction, memory));
    }

    // Terms and fields parsing

    public static long getMemory(Document document) {
        return Long.parseLong(document.get(MEMORY_FIELD));
    }

    public static String getDocumentId(Document document) {
        return document.get(DOCID_FIELD);
    }

    public static Locale getContentFieldLanguage(String fieldName) {
        if (fieldName.startsWith(CONTENT_PREFIX_FIELD))
            return Locale.forLanguageTag(fieldName.substring(CONTENT_PREFIX_FIELD.length()));
        else
            return null;
    }

    // Document creation

    public static Document createDocument(LanguagePair direction, Corpus corpus) throws IOException {
        return createDocument(direction, 0L, corpus);
    }

    public static Document createDocument(LanguagePair direction, long memory, Corpus corpus) throws IOException {
        Reader reader = corpus.getRawContentReader();
        return createDocument(direction, memory, reader);
    }

    public static Document createDocument(LanguagePair direction, long memory, Reader contentReader) {
        Document document = new Document();
        document.add(new StringField(DOCID_FIELD, makeDocumentId(direction, memory), Field.Store.NO));
        document.add(new LongField(MEMORY_FIELD, memory, Field.Store.YES));
        document.add(new StringField(LANGUAGE_FIELD, makeLanguageTag(direction), Field.Store.NO));
        document.add(new CorpusContentField(getContentFieldName(direction), contentReader));

        return document;
    }

    // Utils

    private static String makeDocumentId(LanguagePair direction, long memory) {
        return Long.toString(memory) + "::" + direction.source.toLanguageTag() + "::" + direction.target.toLanguageTag();
    }

    public static String makeLanguageTag(LanguagePair direction) {
        String l1 = direction.source.toLanguageTag();
        String l2 = direction.target.toLanguageTag();

        if (l1.compareTo(l2) > 0) {
            String tmp = l1;
            l1 = l2;
            l2 = tmp;
        }

        return l1 + "__" + l2;
    }

}
