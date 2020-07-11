package eu.modernmt.data;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.TranslationUnit;

import java.util.UUID;

/**
 * Created by davide on 06/09/16.
 */
public class TranslationUnitMessage extends DataMessage {

    public final long memory;
    public final UUID owner;

    public final TranslationUnit value;
    public final boolean update;
    public final String previousSentence;
    public final String previousTranslation;

    public final LanguageDirection language;  // this is the language mapped by the engine's language index
    public final Sentence sentence;
    public final Sentence translation;
    public final Alignment alignment;

    public TranslationUnitMessage(short channel, long channelPosition, long memory, UUID owner, TranslationUnit value,
                                  boolean update, String previousSentence, String previousTranslation,
                                  LanguageDirection language, Sentence sentence, Sentence translation, Alignment alignment) {
        super(channel, channelPosition);
        this.memory = memory;
        this.owner = owner;

        this.value = value;
        this.update = update;
        this.previousSentence = previousSentence;
        this.previousTranslation = previousTranslation;

        this.language = language;
        this.sentence = sentence;
        this.translation = translation;
        this.alignment = alignment;
    }

    @Override
    public String toString() {
        return "TranslationUnitMessage{" +
                "memory=" + memory +
                ", owner=" + owner +
                ", value=" + value +
                ", update=" + update +
                ", previousSentence='" + previousSentence + '\'' +
                ", previousTranslation='" + previousTranslation + '\'' +
                ", language=" + language +
                '}';
    }
}
