package eu.modernmt.config;

/**
 * Created by davide on 24/11/17.
 */
public class TranslationQueueConfig {

    private int highPrioritySize = 512;
    private int normalPrioritySize = 1024;
    private int backgroundPrioritySize = 4096;

    public int getHighPrioritySize() {
        return highPrioritySize;
    }

    public void setHighPrioritySize(int highPrioritySize) {
        this.highPrioritySize = highPrioritySize;
    }

    public int getNormalPrioritySize() {
        return normalPrioritySize;
    }

    public void setNormalPrioritySize(int normalPrioritySize) {
        this.normalPrioritySize = normalPrioritySize;
    }

    public int getBackgroundPrioritySize() {
        return backgroundPrioritySize;
    }

    public void setBackgroundPrioritySize(int backgroundPrioritySize) {
        this.backgroundPrioritySize = backgroundPrioritySize;
    }

    @Override
    public String toString() {
        return "[TranslationQueue]\n" +
                "  high = " + highPrioritySize + "\n" +
                "  normal = " + normalPrioritySize + "\n" +
                "  background = " + backgroundPrioritySize;
    }
}
