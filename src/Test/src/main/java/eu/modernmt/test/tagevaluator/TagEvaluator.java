package eu.modernmt.test.tagevaluator;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.model.FileCorpus;
import eu.modernmt.test.contextanalyzer.context.ContextReader;
import eu.modernmt.test.contextanalyzer.context.Stats;
import org.apache.commons.cli.*;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class TagEvaluator {

    public static final String NAME = "Tag Evaluator";
    public static final String DESCRIPTION = "It evaluates the quality and the precision of the tag management system.";
    public static final double MIN_PRECISION = 0.7;
    private static final Options cliOptions;

    static {
        Option source = Option.builder("source").hasArg().required(true).build();
        Option reference = Option.builder("reference").hasArg().required(true).build();
        Option mt = Option.builder("mt").hasArg().required(true).build();
        cliOptions = new Options();
        cliOptions.addOption(source);
        cliOptions.addOption(reference);
        cliOptions.addOption(mt);
    }

    public static void main(String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(cliOptions, args);
        String sourcePath = cmd.getOptionValue("source");
        String referencePath = cmd.getOptionValue("reference");
        String mtOutPath = cmd.getOptionValue("mt");
        TagEvaluator precisionTest = new TagEvaluator();
        precisionTest.execute(sourcePath, referencePath, mtOutPath);
    }

    private Logger logger;
    private JSONObject jsonResult;

    public TagEvaluator() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.jsonResult = new JSONObject();
        this.jsonResult.put("name", NAME);
        this.jsonResult.put("description", DESCRIPTION);
        this.jsonResult.put("passed", false);
        this.jsonResult.put("results", null);
    }

    public void execute(String sourcePath, String referencePath, String mtOutPath) {
        try {
            File sourceFile = new File(sourcePath);
            File referenceFile = new File(referencePath);
            File mtOutFile = new File(mtOutPath);


            boolean passed = true;
            this.jsonResult.put("passed", passed);
            //this.jsonResult.put("results", stats.getJson());
        } catch (Exception e) {
            e.printStackTrace();
            this.jsonResult.put("error", e.getMessage());
        } finally {
            this.close();
            System.out.println(this.jsonResult.toJSONString());
        }
    }

    public boolean check(Stats stats) {
        return true;
    }

    public void close() {
        logger.info("Resource closed");
    }
}
