package eu.modernmt.data;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 06/09/16.
 */
public class TranslationUnit extends DataMessage {

    public final long domain;

    public LanguagePair direction;
    public String rawSourceSentence;
    public String rawTargetSentence;

    public Sentence sourceSentence = null;
    public Sentence targetSentence = null;
    public Alignment alignment = null;

    public TranslationUnit(short channel, long channelPosition, LanguagePair direction, long domain, String rawSourceSentence, String rawTargetSentence) {
        super(channel, channelPosition);
        this.domain = domain;
        this.direction = direction;
        this.rawSourceSentence = rawSourceSentence;
        this.rawTargetSentence = rawTargetSentence;
    }

}
