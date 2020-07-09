package eu.modernmt.data;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

import java.util.Date;
import java.util.UUID;

/**
 * Created by davide on 06/09/16.
 */
public class TranslationUnitMessage extends DataMessage {

    public final long memory;
    public final UUID owner;

    //TODO: should have "tuid" and "update" parameters
    public final LanguageDirection rawLanguage;  // this is the original language tag from the translation unit
    public final LanguageDirection language;  // this is the language mapped by the engine's language index
    public final String rawSentence;
    public final String rawTranslation;
    public final String rawPreviousSentence;
    public final String rawPreviousTranslation;
    public final Date timestamp;

    public final Sentence sentence;
    public final Sentence translation;
    public final Alignment alignment;

    public TranslationUnitMessage(short channel, long channelPosition, UUID owner, LanguageDirection rawLanguage, LanguageDirection language, long memory,
                                  String rawSentence, String rawTranslation, String rawPreviousSentence, String rawPreviousTranslation,
                                  Date timestamp, Sentence sentence, Sentence translation, Alignment alignment) {
        super(channel, channelPosition);
        this.memory = memory;
        this.owner = owner;
        this.rawLanguage = rawLanguage;
        this.language = language;
        this.rawSentence = rawSentence;
        this.rawTranslation = rawTranslation;
        this.rawPreviousSentence = rawPreviousSentence;
        this.rawPreviousTranslation = rawPreviousTranslation;
        this.timestamp = timestamp;

        this.sentence = sentence;
        this.translation = translation;
        this.alignment = alignment;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('<');
        builder.append(memory);
        builder.append(':');
        builder.append(language);
        builder.append(':');
        builder.append(rawSentence);
        builder.append(':');
        builder.append(rawTranslation);

        if (rawPreviousSentence != null) {
            builder.append(':');
            builder.append(rawPreviousSentence);
            builder.append(':');
            builder.append(rawPreviousTranslation);
        }
        builder.append(':');
        builder.append(timestamp);

        builder.append('>');

        return builder.toString();
    }
}
