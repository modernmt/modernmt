package eu.modernmt.data;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 06/09/16.
 */
public class TranslationUnit extends DataMessage {

    public final int domain;

    public final String originalSourceSentence;
    public final String originalTargetSentence;

    public Sentence sourceSentence = null;
    public Sentence targetSentence = null;
    public Alignment alignment = null;

    public TranslationUnit(short channel, long channelPosition, int domain, String originalSourceSentence, String originalTargetSentence) {
        super(channel, channelPosition);
        this.domain = domain;
        this.originalSourceSentence = originalSourceSentence;
        this.originalTargetSentence = originalTargetSentence;
    }

}
