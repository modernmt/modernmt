package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.io.TokensOutputStream;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.*;

import java.io.IOException;

/**
 * Created by davide on 24/05/17.
 */
class SentenceQueryBuilder {

    public Query build(LanguagePair direction, Sentence sentence) {
        int length = sentence.getWords().length;
        boolean isLongQuery = length > 4;

        int minMatches = isLongQuery ? Math.max(1, (int) (length * .33)) : 1;
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
        String text = TokensOutputStream.toString(sentence, false, true);

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
