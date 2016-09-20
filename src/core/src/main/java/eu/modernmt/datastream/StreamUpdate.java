package eu.modernmt.datastream;

import eu.modernmt.updating.Update;

/**
 * Created by davide on 06/09/16.
 */
class StreamUpdate {

    private final int domain;
    private final String sourceSentence;
    private final String targetSentence;

    private boolean last;
    private boolean first;

    public StreamUpdate(int domain, String sourceSentence, String targetSentence) {
        this(domain, sourceSentence, targetSentence, false, false);
    }

    public StreamUpdate(int domain, String sourceSentence, String targetSentence, boolean last, boolean first) {
        this.domain = domain;
        this.sourceSentence = sourceSentence;
        this.targetSentence = targetSentence;
        this.last = last;
        this.first = first;
    }

    public Update toUpdate(int topicId, long sequentialId) {
        return new Update(topicId, sequentialId, domain, last, first);
    }

    public int getDomain() {
        return domain;
    }

    public String getSourceSentence() {
        return sourceSentence;
    }

    public String getTargetSentence() {
        return targetSentence;
    }

    public boolean isLast() {
        return last;
    }

    public boolean isFirst() {
        return first;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    @Override
    public String toString() {
        return "<" + domain + ":\"" + sourceSentence + "\",\"" + targetSentence + "\">";
    }
}
