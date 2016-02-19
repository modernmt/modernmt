package eu.modernmt.test.contextanalyzer.context;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.model.FileCorpus;
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

/**
 * Created by lucamastrostefano on 17/02/16.
 */
public class PrecisionTest {

    public static final String NAME = "Context Analyzer Precision Test";
    public static final String DESCRIPTION = "It creates an instance of the context analyzer and queries it to compute some metrics to evaluate the efficiency of the IR system.";
    public static final double MIN_PRECISION = 0.7;
    private static final int DEFAULT_LINES_COUNT = 30;
    private static final int DEFAULT_NUMBER_OF_RESULTS = 10;
    private static final Options cliOptions;

    static {
        Option language = Option.builder("lang").hasArg().required(true).build();
        Option trainDataPath = Option.builder("train").hasArg().required(true).build();
        Option testDataPath = Option.builder("test").hasArg().required(true).build();
        Option devDataPath = Option.builder("dev").hasArg().required(false).build();
        Option lines = Option.builder("lines").required(false).hasArg().type(Number.class).build();
        Option numberOfResults = Option.builder("results").required(false).hasArg().type(Number.class).build();
        cliOptions = new Options();
        cliOptions.addOption(language);
        cliOptions.addOption(trainDataPath);
        cliOptions.addOption(testDataPath);
        cliOptions.addOption(devDataPath);
        cliOptions.addOption(lines);
        cliOptions.addOption(numberOfResults);

    }

    public static void main(String[] args) throws ParseException {
        CommandLine cmd = new DefaultParser().parse(cliOptions, args);
        String lang = cmd.getOptionValue("lang");
        String trainDataPath = cmd.getOptionValue("train");
        String testDataPath = cmd.getOptionValue("test");
        String devDataPath = cmd.hasOption("dev") ? cmd.getOptionValue("dev") : null;
        int linesCount;
        if (cmd.hasOption("lines")) {
            linesCount = ((Number) cmd.getParsedOptionValue("lines")).intValue();
        } else {
            linesCount = DEFAULT_LINES_COUNT;
        }
        int numberOfResults;
        if (cmd.hasOption("results")) {
            numberOfResults = ((Number) cmd.getParsedOptionValue("results")).intValue();
        } else {
            numberOfResults = DEFAULT_NUMBER_OF_RESULTS;
        }
        PrecisionTest precisionTest = new PrecisionTest();
        precisionTest.execute(Locale.forLanguageTag(lang), trainDataPath, testDataPath, devDataPath, linesCount, numberOfResults);
    }

    private final Logger logger;
    private File tempIndex;
    private ContextAnalyzer contextAnalyzer;
    private JSONObject jsonResult;

    public PrecisionTest() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.tempIndex = null;
        this.contextAnalyzer = null;
        this.jsonResult = new JSONObject();
        this.jsonResult.put("name", NAME);
        this.jsonResult.put("description", DESCRIPTION);
        this.jsonResult.put("passed", false);
        this.jsonResult.put("results", null);
    }

    public void execute(Locale lang, String trainDataPath, String testDataPath, String devDataPath, int contextLinesCount, int numberOfResults) {
        try {
            this.initializeContextAnalyzer();
            this.indexContex(lang,trainDataPath);
            Stats stats = this.test(lang, testDataPath, contextLinesCount, numberOfResults);
            boolean passed = this.check(stats);
            this.jsonResult.put("passed", passed);
            this.jsonResult.put("results", stats.getJson());
        } catch (Exception e) {
            e.printStackTrace();
            this.jsonResult.put("error", e.getMessage());
        } finally {
            this.close();
            System.out.println(this.jsonResult.toJSONString());
        }
    }

    public void initializeContextAnalyzer() throws IOException {
        logger.info("Start creating the index...");

        Path tempIndexPath = Files.createTempDirectory("contextAnalyzerIndex");
        this.tempIndex = tempIndexPath.toFile();
        this.tempIndex.deleteOnExit();
        logger.debug("Created temp directory at " + this.tempIndex.getAbsolutePath());
        //System.out.println("Created temp directory at " + this.tempIndex.getAbsolutePath());

        this.contextAnalyzer = new ContextAnalyzer(this.tempIndex);
        logger.info("Index created!");
    }

    public void indexContex(Locale lang, String trainDataPath) throws IOException, ContextAnalyzerException {
        logger.info("Start indexing corpora...");
        File directory = new File(trainDataPath);
        this.contextAnalyzer.rebuild(FileCorpus.list(directory, lang.toLanguageTag()));
        logger.info("Corpora indexed!");
    }

    public Stats test(Locale lang, String testDataPath, int contextLinesCount, int numberOfResults) throws IOException, ContextAnalyzerException {
        logger.info("Start querying the index...");
        File directory = new File(testDataPath);
        Stats stats = new Stats();
        Collection<FileCorpus> domains = FileCorpus.list(directory, lang.toLanguageTag());
        int numberOfAnalyzedDomains = 0;
        for (FileCorpus fileCorpus : domains) {
            numberOfAnalyzedDomains++;
            String domainId = fileCorpus.getName();
            logger.debug(numberOfAnalyzedDomains + "/" + domains.size() + "] Testing context analyzer with " + domainId);
            //System.out.println(numberOfAnalyzedDomains + "/" + domains.size() + "] Testing context analyzer with " + domainId);
            ContextReader contextReader = new ContextReader(fileCorpus, contextLinesCount);

            while (contextReader.hasNext()) {
                String context = contextReader.next();
                long startTime = System.currentTimeMillis();
                List<ContextDocument> response = contextAnalyzer.getContext(context, lang, numberOfResults);
                long queryTime = System.currentTimeMillis() - startTime;
                stats.addSample(response, domainId, queryTime);
                //if (Math.random() < 0.05) {
                //    System.out.println(domainId + " " + stats.toString());
                //}
            }
            contextReader.close();
        }
        return stats;
    }

    public boolean check(Stats stats) {
        return stats.numberOfMatches / (double) stats.numberOfQuery >= MIN_PRECISION;
    }

    public void close() {
        logger.info("Deleting index...");
        if (this.tempIndex != null) {
            this.tempIndex.delete();
        }
        logger.info("Index deleted!");
    }
}
