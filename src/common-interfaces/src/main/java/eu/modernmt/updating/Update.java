package eu.modernmt.updating;

import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;

/**
 * Created by davide on 06/09/16.
 */
public class Update {

    public final int domain;
    public final boolean last;
    public final boolean first;
    public final int streamId;
    public final long sentenceId;

    public final String originalSourceSentence;
    public final String originalTargetSentence;

    public Sentence sourceSentence = null;
    public Sentence targetSentence = null;
    public Alignment alignment = null;

    public Update(int streamId, long sentenceId, int domain, boolean last, boolean first, String originalSourceSentence, String originalTargetSentence) {
        this.domain = domain;
        this.last = last;
        this.first = first;
        this.streamId = streamId;
        this.sentenceId = sentenceId;
        this.originalSourceSentence = originalSourceSentence;
        this.originalTargetSentence = originalTargetSentence;
    }

}
