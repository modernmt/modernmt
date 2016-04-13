package eu.modernmt.processing.detokenizer.moses;

import eu.modernmt.config.Config;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.model.Translation;
import eu.modernmt.model.Word;
import eu.modernmt.processing.detokenizer.Detokenizer;
import eu.modernmt.processing.detokenizer.MultiInstanceDetokenizer;
import eu.modernmt.processing.framework.ProcessingException;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Map;

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

    private static class MosesDetokenizerImplementation implements AutoCloseable, Detokenizer {

        private Process detokenizer = null;
        private OutputStream detokenizerStdin;
        private UnixLineReader detokenizerStdout = null;

        public MosesDetokenizerImplementation(String languageCode) {
            File moses = new File(Config.fs.tokenizerModels, "moses");
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
        public Translation call(Translation translation, Map<String, Object> metadata) throws ProcessingException {
            String detokenized = detokenize(translation.getStrippedString(false));
            int length = detokenized.length();

            int stringIndex = 0;
            for (Word word : translation.getWords()) {
                String tokenText = word.getText();

                int position = detokenized.indexOf(tokenText, stringIndex);
                stringIndex = position + tokenText.length();

                word.setRightSpace(stringIndex < length && detokenized.charAt(stringIndex) == ' ' ? " " : null);
            }

            return translation;
        }

        private String detokenize(String text) throws ProcessingException {
            try {
                this.detokenizerStdin.write(text.getBytes("utf-8"));
                this.detokenizerStdin.write('\n');
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
