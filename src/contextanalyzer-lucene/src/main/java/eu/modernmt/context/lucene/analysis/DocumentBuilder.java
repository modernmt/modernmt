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

    private static final String DOCID_FIELD = "doc_id";
    private static final String DOMAIN_FIELD = "domain";
    private static final String CONTENT_FIELD = "content";

    // Fields

    public static String getContentFieldName(LanguagePair direction) {
        return CONTENT_FIELD + "::" + direction.source.toLanguageTag() + "::" + direction.target.toLanguageTag();
    }

    // Terms

    public static Term makeDocumentIdTerm(LanguagePair direction, long domain) {
        return new Term(DOCID_FIELD, makeDocumentId(direction, domain));
    }

    public static Term makeDomainTerm(long domain) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(domain, 0, builder);

        return new Term(DOMAIN_FIELD, builder.toBytesRef());
    }

    private static String makeDocumentId(LanguagePair direction, long domain) {
        return Long.toString(domain) + "::" + direction.source.toLanguageTag() + "::" + direction.target.toLanguageTag();
    }

    // Terms and fields parsing

    public static long getDomain(Document document) {
        return Long.parseLong(document.get(DOMAIN_FIELD));
    }

    public static Locale getSourceLanguage(String fieldName) {
        String[] parts = fieldName.split("::");

        if (parts.length != 3 || !parts[0].equals(CONTENT_FIELD))
            throw new IllegalArgumentException("Field '" + fieldName + "' is not a content field");

        return Locale.forLanguageTag(parts[1]);
    }

    // Document creation

    public static Document createDocument(LanguagePair direction, Corpus corpus) throws IOException {
        return createDocument(direction, 0L, corpus);
    }

    public static Document createDocument(LanguagePair direction, long domain, Corpus corpus) throws IOException {
        Reader reader = corpus.getRawContentReader();
        return createDocument(direction, domain, reader);
    }

    public static Document createDocument(LanguagePair direction, long domain, Reader contentReader) {
        Document document = new Document();
        document.add(new StringField(DOCID_FIELD, makeDocumentId(direction, domain), Field.Store.NO));
        document.add(new LongField(DOMAIN_FIELD, domain, Field.Store.YES));
        document.add(new CorpusContentField(getContentFieldName(direction), contentReader));

        return document;
    }

}
