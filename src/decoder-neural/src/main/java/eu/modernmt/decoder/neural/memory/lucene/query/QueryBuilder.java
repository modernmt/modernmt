package eu.modernmt.decoder.neural.memory.lucene.query;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 * Created by davide on 24/05/17.
 */
public interface QueryBuilder {

    Query getByHash(long memory, LanguagePair direction, String hash);

    Term memoryTerm(long memory);

    Term channelsTerm();

    Query bestMatchingSuggestion(LanguagePair direction, Sentence sentence);

}
