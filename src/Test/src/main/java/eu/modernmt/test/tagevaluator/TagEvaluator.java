package eu.modernmt.test.tagevaluator;

import org.apache.commons.cli.*;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class TagEvaluator {

    public static final String NAME = "Tag Evaluator";
    public static final String DESCRIPTION = "It evaluates the quality of the tag management by calculating the tag-error-rate of an hypothesis file with respect to a reference file.";
    private static final Options cliOptions;
    private static boolean DebugFlag = false;
    private static final String EVALUATOR_CMD = System.getProperty("mmt.home") + "/src/Test/opt/tagevaluator/go-ter.sh";
    private static final String OUT_FILE = System.getProperty("mmt.home") + "/src/Test/opt/tagevaluator/scores/trial";

    private Logger logger;
    private JSONObject jsonResult;

    static {
        cliOptions = new Options();
	cliOptions.addOption("h", "help", false, "display usage information");
	cliOptions.addOption("l", "log_file", true, "the file to save the log info");
        cliOptions.addOption("t", "option_on_tag", true, "the way tag are processed: must be one of [none|separate|countSpaces]");
	cliOptions.addOption("r", "ref_file", true, "the reference file");
	cliOptions.addOption("y", "hyp_file", true, "the hypothesis file");
	cliOptions.addOption("v", "verbosity_level", true, "the verbosity level: should be 0=default,1,2");
    }

    public static void main(String[] args) throws ParseException, IOException {
        CommandLine cmd = new DefaultParser().parse(cliOptions, args);
	if(cmd.hasOption("h")) {printUsageAndExit(cliOptions);}
	if(! cmd.hasOption("t") || ! cmd.hasOption("r") || ! cmd.hasOption("y")) {
	    printUsageAndExit(cliOptions);
	}
	String optionOnTag = cmd.getOptionValue("t");
        String refFile = cmd.getOptionValue("r");
        String hypFile = cmd.getOptionValue("y");

        String logFile = null;
	if(cmd.hasOption("l")) {logFile = cmd.getOptionValue("l");}
	String verbosityLevel = "0";
	if(cmd.hasOption("v")) {verbosityLevel = cmd.getOptionValue("v");}

	String [] evalCmdList = {EVALUATOR_CMD, optionOnTag, refFile, hypFile, OUT_FILE, verbosityLevel};
        TagEvaluator tagErrorRate = new TagEvaluator();
        tagErrorRate.execute(evalCmdList, logFile);
    }

    public TagEvaluator() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.jsonResult = new JSONObject();
        this.jsonResult.put("name", NAME);
        this.jsonResult.put("description", DESCRIPTION);
        this.jsonResult.put("passed", false);
        this.jsonResult.put("results", null);
    }

    public void execute(String[] evalCmdList, String logFile) throws IOException {
	JSONObject jResultDetails = new JSONObject();
	jResultDetails.put("tagErrorRate", null);
	ProcessBuilder pb = new ProcessBuilder(evalCmdList);
	Process p = pb.start();
	BufferedReader In = new BufferedReader(new InputStreamReader(p.getInputStream()));
	String resultBuf = In.readLine();
        try {
	    float tagErrorRate = Float.parseFloat (resultBuf);
	    jResultDetails.put("tagErrorRate", tagErrorRate);
            this.jsonResult.put("passed", true);
	} catch (Exception e) {
	    String line;
	    resultBuf += '\n';
	    while ((line = In.readLine()) != null) {
		resultBuf += line;
	    }
            boolean passed = false;
            this.jsonResult.put("passed", false);
            this.jsonResult.put("error", resultBuf);
        }

	if (logFile != null) {
	    BufferedReader Err = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	    PrintWriter out = new PrintWriter(new FileWriter(logFile));
	    String line;
	    while ((line = Err.readLine()) != null) {
		out.println(line);
	    }
	    out.close();
	    jResultDetails.put("logFile", logFile);
	}

	this.jsonResult.put("results", jResultDetails);
	
	this.close();
	System.out.println(this.jsonResult.toJSONString());
    }

    public void close() {
        logger.info("Resource closed");
    }

    private static void printUsageAndExit(Options options) {
        int WIDTH = 80;
	
	StringWriter stringWriter = new StringWriter();
	PrintWriter writer = new PrintWriter(stringWriter);
	final HelpFormatter formatter = new HelpFormatter();

	// 1) formatter.printOptions(writer, WIDTH, options, 2, 2);
        // 2) formatter.printUsage(writer, WIDTH, "eu.modernmt.test.tagevaluator.TagEvaluator", options);
	formatter.printHelp(writer, WIDTH, "eu.modernmt.test.tagevaluator.TagEvaluator", "", options, 2, 2, "");
	String formattedString = stringWriter.toString();
	
        // System.err.println(formattedString);
	// System.err.flush();
	
        JSONObject jsonResult = new JSONObject();
        jsonResult.put("name", NAME);
        jsonResult.put("description", DESCRIPTION);
        jsonResult.put("passed", false);
        jsonResult.put("error", formattedString);
        jsonResult.put("results", null);

	System.out.println(jsonResult.toJSONString());
	System.exit(0);
    }

}
