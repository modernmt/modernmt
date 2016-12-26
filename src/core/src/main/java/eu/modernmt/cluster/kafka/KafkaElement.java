package eu.modernmt.cluster.kafka;

import eu.modernmt.data.TranslationUnit;

/**
 * Created by davide on 06/09/16.
 */
class KafkaElement {

    private final int domain;
    private final String sourceSentence;
    private final String targetSentence;

    public KafkaElement(int domain, String sourceSentence, String targetSentence) {
        this.domain = domain;
        this.sourceSentence = sourceSentence;
        this.targetSentence = targetSentence;
    }

    public TranslationUnit toTranslationUnit(short channel, long position) {
        return new TranslationUnit(channel, position, domain, sourceSentence, targetSentence);
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
