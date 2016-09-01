package eu.modernmt.decoder.moses;

import eu.modernmt.decoder.Decoder;
import eu.modernmt.decoder.DecoderFactory;
import eu.modernmt.io.Paths;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 09/05/16.
 */
public class MosesDecoderFactory extends DecoderFactory {

    @Override
    public Decoder create() throws IOException {
        File iniTemplate = Paths.join(enginePath, "models", "moses.ini");
        MosesINI mosesINI = MosesINI.load(iniTemplate, enginePath);

        if (featureWeights != null)
            mosesINI.setWeights(featureWeights);

        mosesINI.setThreads(decoderThreads);

        File inifile = new File(runtimePath, "moses.ini");
        FileUtils.write(inifile, mosesINI.toString(), false);
        return new MosesDecoder(inifile);
    }

}
