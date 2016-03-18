package eu.modernmt;

import eu.modernmt.config.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
class AlignerProcess implements Closeable{

    private static final String forwardModelFileName = "model.align.fwd";
    private static final String backwardModelFileName = "model.align.bwd";
    private static final Logger logger = LogManager.getLogger(AlignerProcess.class);
    private static final String LAST_LINE = "Loading ttable finished.";
    private static final List<String> EXPECTED_OUTPUT;

    static{
        EXPECTED_OUTPUT = new ArrayList<>();
        EXPECTED_OUTPUT.add("ARG=[a-zA-Z]");
        EXPECTED_OUTPUT.add("\\s+DICT SIZE:\\s+[0-9]{1,11}");
        EXPECTED_OUTPUT.add("Reading Lexical Translation Table");
        EXPECTED_OUTPUT.add(LAST_LINE);
    }


    final boolean reverse;
    final String[] command;
    boolean init = false;
    OutputStream standardInput;
    BufferedReader standardOutput;
    BufferedReader standardError;
    Process process;

    AlignerProcess(boolean reverse, String enginePath){
        this.reverse = reverse;

        String fastAlignPath = new File(Config.fs.home, "opt" + File.separatorChar +
                "bin" + File.separatorChar + "fastalign-maurobuild" + File.separatorChar + "fast_align")
                .getAbsolutePath();
        File modelsFile = new File(enginePath,
                "models" + File.separatorChar + "phrase_tables");
        String modelFileName;
        if(reverse) {
            modelFileName = backwardModelFileName;
        }else{
            modelFileName = forwardModelFileName;
        }
        String modelFilePath = new File(modelsFile.getAbsolutePath(), modelFileName).getAbsolutePath();

        if(reverse) {
            this.command = new String[]{fastAlignPath, "-d", "-v", "-o", "-B", "-f", modelFilePath,
                    "-n", "1", "-b", "0", "-r"};
        }else {
            this.command = new String[]{fastAlignPath, "-d", "-v", "-o", "-B", "-f", modelFilePath,
                    "-n", "1", "-b", "0"};
        }
    }

    void run() throws IOException, ParseException {
        if(this.init){
            throw new IllegalStateException("Fast Align is already initialized");
        }

        Runtime rt = Runtime.getRuntime();
        this.process = rt.exec(this.command);
        standardOutput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        standardInput = process.getOutputStream();
        standardError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        try {
            this.checkRun(standardError);
        }catch(ParseException e){
            throw e;
        }

        this.init = true;
    }

    private void checkRun(BufferedReader standardError) throws IOException, ParseException {
        //Consume and check the standard error of the process
        int expectedOuputIndex = 0;
        int lineNumber = 0;
        String line;
        try {
            while ((line = standardError.readLine()) != null) {
                lineNumber++;
                String expectedOutput = EXPECTED_OUTPUT.get(expectedOuputIndex);
                if (!line.matches(expectedOutput)) {
                    expectedOuputIndex++;
                    expectedOutput = EXPECTED_OUTPUT.get(expectedOuputIndex);
                    if (!line.matches(expectedOutput)) {
                        logger.error("FOUND: \"" + line + "\" REGEX_EXPECTED: " + expectedOutput);
                        throw new ParseException("Cannot parse the standard error of Fast Align", lineNumber);
                    }
                }
                if (line.equals(LAST_LINE)) {
                    break;
                }
            }
        }catch(IndexOutOfBoundsException e){
            throw new ParseException("Fast Align has produced more lines then expected on the standard error", lineNumber);
        }
    }

    boolean isReverse(){
        return reverse;
    }

    @Override
    public void close() throws IOException {
        Closeable[] resources = new Closeable[]{
                this.standardOutput,
                this.standardInput,
                this.standardError
        };
        for (Closeable resource : resources) {
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (Exception e) {
            }
        }
        process.destroy();
    }
}
