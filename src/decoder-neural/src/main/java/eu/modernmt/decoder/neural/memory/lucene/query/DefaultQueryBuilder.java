package eu.modernmt.decoder.neural.memory.lucene.query;

import eu.modernmt.decoder.neural.memory.lucene.Analyzers;
import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by davide on 24/05/17.
 */
public class DefaultQueryBuilder implements QueryBuilder {

    @Override
    public Query getByHash(long memory, String hash) {
        PhraseQuery hashQuery = new PhraseQuery();
        for (String h : hash.split(" "))
            hashQuery.add(DocumentBuilder.makeHashTerm(h));

        TermQuery memoryQuery = new TermQuery(DocumentBuilder.makeMemoryTerm(memory));

        BooleanQuery query = new BooleanQuery();
        query.add(hashQuery, BooleanClause.Occur.MUST);
        query.add(memoryQuery, BooleanClause.Occur.MUST);

        return query;
    }

    @Override
    public Query bestMatchingSuggestion(UUID user, LanguagePair direction, Sentence sentence, ContextVector context) {
        int length = sentence.getWords().length;
        boolean isLongQuery = length > 4;

        int minMatches = isLongQuery ? Math.max(1, (int) (length * .5)) : length;
        Analyzer analyzer = isLongQuery ? Analyzers.getLongQueryAnalyzer() : Analyzers.getShortQueryAnalyzer();

        // Content query
        BooleanQuery termsQuery = makeTermsQuery(direction, sentence, analyzer);
        termsQuery.setMinimumNumberShouldMatch(minMatches);

        // Owner filter
        BooleanQuery privacyQuery = makePrivacyQuery(user, context);

        // Result
        return new FilteredQuery(termsQuery, new QueryWrapperFilter(privacyQuery));
    }

    protected static BooleanQuery makePrivacyQuery(UUID user, ContextVector context) {
        BooleanQuery privacyQuery = new BooleanQuery();

        if (user == null) {
            privacyQuery.add(DocumentBuilder.makePublicOwnerMatchingQuery(), BooleanClause.Occur.SHOULD);
        } else {
            privacyQuery.add(DocumentBuilder.makePublicOwnerMatchingQuery(), BooleanClause.Occur.SHOULD);
            privacyQuery.add(DocumentBuilder.makeOwnerMatchingQuery(user), BooleanClause.Occur.SHOULD);
        }

        if (context != null) {
            for (ContextVector.Entry entry : context) {
                privacyQuery.add(new TermQuery(DocumentBuilder.makeMemoryTerm(entry.memory.getId())), BooleanClause.Occur.SHOULD);
            }
        }

        privacyQuery.setMinimumNumberShouldMatch(1);

        return privacyQuery;
    }

    protected static BooleanQuery makeTermsQuery(LanguagePair direction, Sentence sentence, Analyzer analyzer) {
        BooleanQuery termsQuery = new BooleanQuery();
        loadTerms(DocumentBuilder.makeContentFieldName(direction), sentence, analyzer, termsQuery);
        return termsQuery;
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

}
