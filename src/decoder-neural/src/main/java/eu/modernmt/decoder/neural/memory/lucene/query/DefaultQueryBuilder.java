package eu.modernmt.decoder.neural.memory.lucene.query;

import eu.modernmt.decoder.neural.memory.lucene.Analyzers;
import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRefBuilder;
import org.apache.lucene.util.NumericUtils;

import java.io.IOException;

/**
 * Created by davide on 24/05/17.
 */
public class DefaultQueryBuilder implements QueryBuilder {

    @Override
    public Query getByHash(long memory, LanguagePair direction, String hash) {
        PhraseQuery hashQuery = new PhraseQuery();
        for (String h : hash.split(" "))
            hashQuery.add(new Term(DocumentBuilder.HASH_FIELD, h));

        TermQuery langQuery = new TermQuery(
                new Term(DocumentBuilder.LANGUAGE_FIELD, DocumentBuilder.encode(direction))
        );

        TermQuery memoryQuery = new TermQuery(
                newLongTerm(DocumentBuilder.MEMORY_ID_FIELD, memory)
        );

        BooleanQuery query = new BooleanQuery();
        query.add(hashQuery, BooleanClause.Occur.MUST);
        query.add(langQuery, BooleanClause.Occur.MUST);
        query.add(memoryQuery, BooleanClause.Occur.MUST);

        return query;
    }

    @Override
    public Term memoryTerm(long memory) {
        return newLongTerm(DocumentBuilder.MEMORY_ID_FIELD, memory);
    }

    @Override
    public Term channelsTerm() {
        return newLongTerm(DocumentBuilder.MEMORY_ID_FIELD, 0);
    }

    @Override
    public Query bestMatchingSuggestion(LanguagePair direction, Sentence sentence) {
        int length = sentence.getWords().length;
        boolean isLongQuery = length > 4;

        int minMatches = isLongQuery ? Math.max(1, (int) (length * .5)) : length;
        Analyzer analyzer = isLongQuery ? Analyzers.getLongQueryAnalyzer() : Analyzers.getShortQueryAnalyzer();

        // Language filter
        TermsFilter langFilter = new TermsFilter(new Term(DocumentBuilder.LANGUAGE_FIELD, DocumentBuilder.encode(direction)));

        // Content query
        BooleanQuery termsQuery = new BooleanQuery();
        loadTerms(DocumentBuilder.getContentFieldName(direction.source), sentence, analyzer, termsQuery);
        termsQuery.setMinimumNumberShouldMatch(minMatches);

        // Main query
        return new FilteredQuery(termsQuery, langFilter);
    }

    private static void loadTerms(String fieldName, Sentence sentence, Analyzer analyzer, BooleanQuery output) {
        String text = TokensOutputStream.serialize(sentence, false, true);

        try {
            TokenStream stream = analyzer.tokenStream("content", text);
            CharTermAttribute charTermAttribute = stream.addAttribute(CharTermAttribute.class);

            stream.reset();
            while (stream.incrementToken()) {
                Term term = new Term(fieldName, charTermAttribute.toString());
                output.add(new TermQuery(term), BooleanClause.Occur.SHOULD);
            }

            stream.end();
            stream.close();
        } catch (IOException e) {
            throw new Error("This should never happen", e);
        }
    }

    private static Term newLongTerm(String field, long value) {
        BytesRefBuilder builder = new BytesRefBuilder();
        NumericUtils.longToPrefixCoded(value, 0, builder);

        return new Term(field, builder.toBytesRef());
    }
}
