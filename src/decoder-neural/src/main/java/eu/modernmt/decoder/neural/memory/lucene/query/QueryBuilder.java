package eu.modernmt.decoder.neural.memory.lucene.query;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.lucene.search.Query;

/**
 * Created by davide on 24/05/17.
 */
public interface QueryBuilder {

    Query getByHash(long memory, String hash);

    Query bestMatchingSuggestion(long user, LanguagePair direction, Sentence sentence, ContextVector context);

}
