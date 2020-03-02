package eu.modernmt.decoder.neural.memory.lucene.query;

import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by davide on 24/05/17.
 */
public class DefaultQueryBuilder implements QueryBuilder {

    public static final int SHORT_QUERY_SIZE = 4;

    @Override
    public boolean isLongQuery(int queryLength) {
        return queryLength > SHORT_QUERY_SIZE;
    }

    @Override
    public Query getByHash(DocumentBuilder builder, long memory, String hash) {
        PhraseQuery hashQuery = new PhraseQuery();
        for (String h : hash.split(" "))
            hashQuery.add(builder.makeHashTerm(h));

        TermQuery memoryQuery = new TermQuery(builder.makeMemoryTerm(memory));

        BooleanQuery query = new BooleanQuery();
        query.add(hashQuery, BooleanClause.Occur.MUST);
        query.add(memoryQuery, BooleanClause.Occur.MUST);

        return query;
    }

    @Override
    public Query getChannels(DocumentBuilder builder) {
        return new TermQuery(builder.makeChannelsTerm());
    }

    @Override
    public Query bestMatchingSuggestion(DocumentBuilder builder, Analyzer analyzer, UUID user, LanguageDirection direction, Sentence sentence, ContextVector context) {
        int length = sentence.getWords().length;
        int minMatches = length > SHORT_QUERY_SIZE ? Math.max(1, (int) (length * .5)) : length;

        // Content query
        BooleanQuery termsQuery = makeTermsQuery(builder, direction, sentence, analyzer);
        termsQuery.setMinimumNumberShouldMatch(minMatches);

        // Context filter
        TermsFilter contextFilter = makeContextFilter(builder, context);

        // Result
        return new FilteredQuery(termsQuery, contextFilter);
    }

    protected static TermsFilter makeContextFilter(DocumentBuilder builder, ContextVector context) {
        ArrayList<Term> terms = new ArrayList<>(context.size());
        for (ContextVector.Entry entry : context)
            terms.add(builder.makeMemoryTerm(entry.memory.getId()));
        return new TermsFilter(terms);
    }

    protected static BooleanQuery makeTermsQuery(DocumentBuilder builder, LanguageDirection direction, Sentence sentence, Analyzer analyzer) {
        BooleanQuery termsQuery = new BooleanQuery();
        loadTerms(builder.makeContentFieldName(direction), sentence, analyzer, termsQuery);
        return termsQuery;
    }

    private static void loadTerms(String fieldName, Sentence sentence, Analyzer analyzer, BooleanQuery output) {
        String text = TokensOutputStream.serialize(sentence, false, true);

        try {
            TokenStream stream = analyzer.tokenStream(fieldName, text);
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

}
