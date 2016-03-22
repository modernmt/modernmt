package eu.modernmt.aligner.fastalign;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.config.Config;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.util.TokensOutputter;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
class FastAlign implements Aligner, Closeable {

    private static final Logger logger = LogManager.getLogger(FastAlign.class);
    private static final String SENTENCE_SEPARATOR = " ||| ";
    private static final String LAST_LINE = "Loading ttable finished.";
    private static final List<String> EXPECTED_OUTPUT;

    static {
        EXPECTED_OUTPUT = new ArrayList<>();
        EXPECTED_OUTPUT.add("ARG=[a-zA-Z]");
        EXPECTED_OUTPUT.add("\\s+DICT SIZE:\\s+[0-9]{1,11}");
        EXPECTED_OUTPUT.add("Reading Lexical Translation Table");
        EXPECTED_OUTPUT.add(LAST_LINE);
    }

    private final boolean reverse;
    private final String[] command;
    private boolean init = false;
    private OutputStream standardInput;
    private BufferedReader standardOutput;
    private BufferedReader standardError;
    private Process process;

    FastAlign(boolean reverse, String modelFilePath) {
        this.reverse = reverse;

        String fastAlignPath = new File(Config.fs.home, "opt" + File.separatorChar +
                "bin" + File.separatorChar + "fastalign-maurobuild" + File.separatorChar + "fast_align")
                .getAbsolutePath();
        if (reverse) {
            this.command = new String[]{fastAlignPath, "-d", "-v", "-o", "-B", "-f", modelFilePath,
                    "-n", "1", "-b", "0", "-r"};
        } else {
            this.command = new String[]{fastAlignPath, "-d", "-v", "-o", "-B", "-f", modelFilePath,
                    "-n", "1", "-b", "0"};
        }
    }

    public static int[][] parseAlignments(String stringAlignments) {
        String[] links_str = stringAlignments.split(" ");
        int[][] alignments = new int[links_str.length][];
        for (int i = 0; i < links_str.length; i++) {
            String[] alignment = links_str[i].split("-");
            alignments[i] = new int[]{Integer.parseInt(alignment[0]), Integer.parseInt(alignment[1])};
        }
        return alignments;
    }

    @Override
    public void init() throws IOException, ParseException {
        if (this.init) {
            throw new IllegalStateException("Fast Align is already initialized");
        }

        Runtime rt = Runtime.getRuntime();
        this.process = rt.exec(this.command, new String[]{
                "LD_LIBRARY_PATH=" + Config.fs.lib
        });
        standardOutput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));
        standardInput = process.getOutputStream();
        standardError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        try {
            this.checkRun(standardError);
        } catch (ParseException e) {
            throw e;
        }

        this.init = true;
    }

    private void checkRun(BufferedReader standardError) throws IOException, ParseException {
        //Consume and check the standard error of the process
        int expectedOutputIndex = 0;
        int lineNumber = 0;
        String line;
        try {
            while ((line = standardError.readLine()) != null) {
                lineNumber++;
                String expectedOutput = EXPECTED_OUTPUT.get(expectedOutputIndex);
                if (!line.matches(expectedOutput)) {
                    expectedOutputIndex++;
                    expectedOutput = EXPECTED_OUTPUT.get(expectedOutputIndex);
                    if (!line.matches(expectedOutput)) {
                        logger.error("FOUND: \"" + line + "\" REGEX_EXPECTED: " + expectedOutput);
                        throw new ParseException("Cannot parse the standard error of Fast Align", lineNumber);
                    }
                }
                if (line.equals(LAST_LINE)) {
                    break;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new ParseException("Fast Align has produced more lines then expected on the standard error", lineNumber);
        }
    }

    protected String getStringAlignments(Sentence sentence, Sentence translation) throws IOException {
        String sentence_str = TokensOutputter.toString(sentence, false, false);
        String translation_str = TokensOutputter.toString(translation, false, false);
        String query = sentence_str + SENTENCE_SEPARATOR + translation_str + "\n";
        logger.debug("Sending query to Fast Align's models: " + query);
        this.standardInput.write(query.getBytes(Config.charset.get()));
        this.standardInput.flush();
        logger.debug("Waiting for alignments");
        String modelResponse = this.standardOutput.readLine();
        logger.debug("Alignments: " + modelResponse);
        return modelResponse;
    }

    @Override
    public int[][] getAlignments(Sentence sentence, Sentence translation) throws IOException {
        return parseAlignments(this.getStringAlignments(sentence, translation));
    }

    @Override
    public void close() throws IOException {
        Closeable[] resources = new Closeable[]{
                this.standardOutput,
                this.standardInput,
                this.standardError
        };
        for (Closeable resource : resources) {
            IOUtils.closeQuietly(resource);
        }
        process.destroy();
    }

}
