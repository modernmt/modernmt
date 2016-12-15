package eu.modernmt.cluster.datastream;

import eu.modernmt.updating.Update;

/**
 * Created by davide on 06/09/16.
 */
class StreamUpdate {

    private final int domain;
    private final String sourceSentence;
    private final String targetSentence;

    public StreamUpdate(int domain, String sourceSentence, String targetSentence) {
        this.domain = domain;
        this.sourceSentence = sourceSentence;
        this.targetSentence = targetSentence;
    }

    public Update toUpdate(int topicId, long sequentialId) {
        return new Update(topicId, sequentialId, domain, sourceSentence, targetSentence);
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

    @Override
    public String toString() {
        return "<" + domain + ":\"" + sourceSentence + "\",\"" + targetSentence + "\">";
    }
}
