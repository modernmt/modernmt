package eu.modernmt.processing.detokenizer.moses;

import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.MultiInstanceDetokenizer;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.framework.UnixLineReader;
import eu.modernmt.processing.tokenizer.util.Environment;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * Created by davide on 27/01/16.
 */
public class MosesDetokenizer extends MultiInstanceDetokenizer {

    private static class MosesDetokenizerFactory implements DetokenizerFactory {

        private String languageCode;

        public MosesDetokenizerFactory(String languageCode) {
            this.languageCode = languageCode;
        }

        @Override
        public Detokenizer newInstance() {
            return new MosesDetokenizerImplementation(languageCode);
        }
    }

    public MosesDetokenizer(String languageCode) {
        super(new MosesDetokenizerFactory(languageCode));
    }

    private static class MosesDetokenizerImplementation implements Detokenizer, AutoCloseable {

        private Process detokenizer = null;
        private OutputStream detokenizerStdin;
        private UnixLineReader detokenizerStdout = null;

        public MosesDetokenizerImplementation(String languageCode) {
            File moses = new File(Environment.MODELS_PATH, "moses");
            File scripts = new File(moses, "scripts");
            File detokenizerScript = new File(scripts, "detokenizer.perl");

            String detokenizerCommand = "perl " + detokenizerScript.getAbsolutePath() + " -b -l " + languageCode;

            Runtime runtime = Runtime.getRuntime();

            try {
                this.detokenizer = runtime.exec(detokenizerCommand);
                this.detokenizerStdin = detokenizer.getOutputStream();
                this.detokenizerStdout = new UnixLineReader(new InputStreamReader(detokenizer.getInputStream(), "UTF-8"));
            } catch (IOException e) {
                this.close();
                throw new RuntimeException("Error while executing processes", e);
            }
        }

        @Override
        public String call(String[] tokens) throws ProcessingException {
            try {
                for (int i = 0; i < tokens.length; i++) {
                    this.detokenizerStdin.write(tokens[i].getBytes("utf-8"));
                    if (i < tokens.length - 1)
                        this.detokenizerStdin.write(' ');
                    else
                        this.detokenizerStdin.write('\n');
                }
                this.detokenizerStdin.flush();

                return this.detokenizerStdout.readLine();
            } catch (IOException e) {
                throw new ProcessingException("Error while running perl script", e);
            }
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(detokenizerStdin);
            IOUtils.closeQuietly(detokenizerStdout);

            if (detokenizer != null)
                detokenizer.destroy();
        }

    }
}
