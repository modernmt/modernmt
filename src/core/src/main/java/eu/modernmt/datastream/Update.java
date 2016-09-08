package eu.modernmt.datastream;

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
    public final long sequentialId;

    public Sentence sourceSentence = null;
    public Sentence targetSentence = null;
    public Alignment alignment = null;

    static Update fromStream(int topicId, long sequentialId, StreamUpdate other) {
        return new Update(topicId, sequentialId, other.getDomain(), other.isLast(), other.isFirst());
    }

    private Update(int streamId, long sequentialId, int domain, boolean last, boolean first) {
        this.domain = domain;
        this.last = last;
        this.first = first;
        this.streamId = streamId;
        this.sequentialId = sequentialId;
    }

}
