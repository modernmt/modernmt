package eu.modernmt.config;

/**
 * Created by davide on 06/07/17.
 */
public class PhraseBasedDecoderConfig extends DecoderConfig {

    @Override
    public int getParallelismDegree() {
        return threads;
    }

    @Override
    public int getThreads() {
        return threads;
    }

    @Override
    public void setThreads(int threads) {
        this.threads = threads;
    }

    @Override
    public String toString() {
        return "[Phrase-based decoder]\n" +
                "  threads = " + threads + "\n" +
                "  enabled = " + enabled;
    }

}
