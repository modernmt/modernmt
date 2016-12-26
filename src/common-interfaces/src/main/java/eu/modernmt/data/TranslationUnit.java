package eu.modernmt.data;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 06/09/16.
 */
public class TranslationUnit {

    public final int domain;
    public final short channel;
    public final long channelPosition;

    public final String originalSourceSentence;
    public final String originalTargetSentence;

    public Sentence sourceSentence = null;
    public Sentence targetSentence = null;
    public Alignment alignment = null;

    public TranslationUnit(short channel, long channelPosition, int domain, String originalSourceSentence, String originalTargetSentence) {
        this.domain = domain;
        this.channel = channel;
        this.channelPosition = channelPosition;
        this.originalSourceSentence = originalSourceSentence;
        this.originalTargetSentence = originalTargetSentence;
    }

}
