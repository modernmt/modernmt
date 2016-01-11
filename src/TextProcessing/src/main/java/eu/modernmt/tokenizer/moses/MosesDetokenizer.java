package eu.modernmt.tokenizer.moses;

import eu.modernmt.tokenizer.IDetokenizer;
import eu.modernmt.tokenizer.ITokenizer;
import eu.modernmt.tokenizer.utils.UnixLineReader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Scanner;

/**
 * Created by davide on 23/11/15.
 */
public class MosesDetokenizer extends IDetokenizer implements Closeable {

    private Process detokenizer = null;
    private OutputStream detokenizerStdin;
    private UnixLineReader detokenizerStdout = null;

    public MosesDetokenizer(String languageCode) {
        File moses = new File(ITokenizer.MODELS_PATH, "moses");
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
    public String detokenize(String[] tokens) {
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
            throw new RuntimeException("Error while running detokenizer", e);
        }

    }

    @Override
    public void close() {
        IOUtils.closeQuietly(detokenizerStdin);
        IOUtils.closeQuietly(detokenizerStdout);

        if (detokenizer != null)
            detokenizer.destroy();
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
    }

}
