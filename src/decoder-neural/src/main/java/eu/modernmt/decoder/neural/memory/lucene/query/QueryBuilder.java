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

    Query getByMatch(DocumentBuilder builder, long memory, LanguageDirection language, String previousSentence, String previousTranslation);

    Query getByTuid(DocumentBuilder builder, long memory, LanguageDirection language, String tuid);

    Query getChannels(DocumentBuilder builder);

    Query bestMatchingSuggestion(DocumentBuilder builder, Analyzer analyzer, UUID user, LanguageDirection direction, Sentence sentence, ContextVector context);

}
