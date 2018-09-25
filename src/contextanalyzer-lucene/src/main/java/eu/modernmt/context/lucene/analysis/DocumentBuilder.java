package eu.modernmt.context.lucene.analysis;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.Corpus;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.UUID;

/**
 * Created by davide on 23/09/15.
 */
public class DocumentBuilder {

    // Factory methods

    public static Document newInstance(LanguagePair direction, Corpus corpus) throws IOException {
        return newInstance(null, 0L, direction, corpus);
    }

    public static Document newInstance(UUID owner, long memory, LanguagePair direction, Corpus corpus) throws IOException {
        return newInstance(owner, memory, direction, corpus.getRawContentReader());
    }

    public static Document newInstance(UUID owner, long memory, LanguagePair direction, Reader contentReader) {
        Document document = new Document();
        document.add(new StringField(DOC_ID_FIELD, makeId(memory, direction), Field.Store.NO));
        document.add(new LongField(MEMORY_FIELD, memory, Field.Store.YES));

        if (owner != null) {
            document.add(new LongField(OWNER_MSB_FIELD, owner.getMostSignificantBits(), Field.Store.NO));
            document.add(new LongField(OWNER_LSB_FIELD, owner.getLeastSignificantBits(), Field.Store.NO));
        } else {
            document.add(new LongField(OWNER_MSB_FIELD, 0L, Field.Store.NO));
            document.add(new LongField(OWNER_LSB_FIELD, 0L, Field.Store.NO));
        }

        document.add(new CorpusContentField(makeContentFieldName(direction), contentReader));

        return document;
    }

    private static final String DOC_ID_FIELD = "cid";
    private static final String MEMORY_FIELD = "memory";
    private static final String OWNER_MSB_FIELD = "owner_msb";
    private static final String OWNER_LSB_FIELD = "owner_lsb";
    private static final String CONTENT_PREFIX_FIELD = "content_";

    // Getters

    public static String getId(Document self) {
        return self.get(DOC_ID_FIELD);
    }

    public static long getMemory(Document self) {
        return Long.parseLong(self.get(MEMORY_FIELD));
    }

    public static long getMemory(String docId) {
        String[] parts = docId.split("_");
        if (parts.length != 3)
            throw new IllegalArgumentException("Invalid Document ID: " + docId);

        return Long.parseLong(parts[0]);
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
        return makeLongTerm(memory, MEMORY_FIELD);
    }

    public static Query makePublicOwnerMatchingQuery() {
        return makeOwnerMatchingQuery(null);
    }

    public static Query makeOwnerMatchingQuery(UUID owner) {
        long msb = (owner != null) ? owner.getMostSignificantBits() : 0L;
        long lsb = (owner != null) ? owner.getLeastSignificantBits() : 0L;

        Term msbTerm = makeLongTerm(msb, OWNER_MSB_FIELD);
        Term lsbTerm = makeLongTerm(lsb, OWNER_LSB_FIELD);

        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(msbTerm), BooleanClause.Occur.MUST);
        query.add(new TermQuery(lsbTerm), BooleanClause.Occur.MUST);
        return query;
    }

    // Value builders

    public static String makeId(long memory, LanguagePair direction) {
        return Long.toString(memory) + '_' + direction.source.getLanguage() + '_' + direction.target.getLanguage();
    }

    // Fields builders

    public static String makeContentFieldName(LanguagePair direction) {
        return CONTENT_PREFIX_FIELD + direction.source.getLanguage() + '_' + direction.target.getLanguage();
    }

    // Utils

    private static Term makeLongTerm(long value, String field) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(value, 0, builder);

        return new Term(field, builder.toBytesRef());
    }

}
