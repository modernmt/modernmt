package eu.modernmt.decoder.neural.memory.lucene;

import eu.modernmt.data.TranslationUnit;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.memory.TranslationMemory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

import java.util.Map;

/**
 * Created by davide on 23/05/17.
 */
public interface DocumentBuilder {

    // Factory methods

    Document create(TranslationUnit unit);

    Document create(Map<Short, Long> channels);

    // Getters

    long getMemory(Document self);

    String getSourceLanguage(String fieldName);

    String getTargetLanguage(String fieldName);

    // Parsing

    ScoreEntry asScoreEntry(Document self);

    ScoreEntry asScoreEntry(Document self, LanguageDirection direction);

    TranslationMemory.Entry asEntry(Document self);

    Map<Short, Long> asChannels(Document self);

    // Term constructors

    Term makeHashTerm(String h);

    Term makeMemoryTerm(long memory);

    Term makeChannelsTerm();

    Term makeLanguageTerm(Language language);

    // Fields builders

    boolean isHashField(String field);

    String makeLanguageFieldName(Language language);

    String makeContentFieldName(LanguageDirection direction);

}
