package eu.modernmt.decoder.neural.memory.lucene.query;

import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Sentence;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

import java.util.UUID;

/**
 * Created by davide on 24/05/17.
 */
public interface QueryBuilder {

    boolean isLongQuery(int queryLength);

    Query getByHash(DocumentBuilder builder, long memory, String hash);

    Query getChannels(DocumentBuilder builder);

    Query bestMatchingSuggestion(DocumentBuilder builder, Analyzer analyzer, UUID user, LanguageDirection direction, Sentence sentence, ContextVector context);

}
