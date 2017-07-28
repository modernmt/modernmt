package eu.modernmt.data;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.LanguagePair;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 06/09/16.
 */
public class TranslationUnit extends DataMessage {

    public final long domain;

    public final LanguagePair direction;
    public final String originalSourceSentence;
    public final String originalTargetSentence;

    public Sentence sourceSentence = null;
    public Sentence targetSentence = null;
    public Alignment alignment = null;

    public TranslationUnit(short channel, long channelPosition, LanguagePair direction, long domain, String originalSourceSentence, String originalTargetSentence) {
        super(channel, channelPosition);
        this.domain = domain;
        this.direction = direction;
        this.originalSourceSentence = originalSourceSentence;
        this.originalTargetSentence = originalTargetSentence;
    }

}
