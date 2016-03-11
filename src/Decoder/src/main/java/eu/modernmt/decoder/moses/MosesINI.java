package eu.modernmt.decoder.moses;

import eu.modernmt.config.Config;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by davide on 02/12/15.
 */
public class MosesINI {

    public static MosesINI load(File templateFile, File enginePath) throws IOException {
        String template = FileUtils.readFileToString(templateFile, Config.charset.get());
        String engine = enginePath.getAbsolutePath();
        if (!engine.endsWith(File.separator))
            engine = engine + File.separator;

        return new MosesINI(template.replace("${ENGINE_PATH}", engine));
    }

    private String template;
    private int threads;
    private Map<String, float[]> weights;

    private MosesINI(String template) {
        this.template = template;
        this.threads = 1;
        this.weights = null;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public int getThreads() {
        return this.threads;
    }

    public void setWeights(Map<String, float[]> weights) {
        this.weights = weights;
    }

    @Override
    public String toString() {
        String ini = template.replace("${DECODER_THREADS}", Integer.toString(threads));

        if (weights != null) {
            StringBuilder weightsSection = new StringBuilder();
            weightsSection.append("\n[weight]\n");

            for (Map.Entry<String, float[]> entry : weights.entrySet()) {
                weightsSection.append(entry.getKey());
                weightsSection.append('=');

                for (float score : entry.getValue()) {
                    weightsSection.append(' ');
                    if (score == MosesFeature.UNTUNEABLE_COMPONENT)
                        weightsSection.append("UNTUNEABLECOMPONENT");
                    else
                        weightsSection.append(score);
                }

                weightsSection.append('\n');
            }

            ini = ini + weightsSection;
        }

        return ini;
    }
}
