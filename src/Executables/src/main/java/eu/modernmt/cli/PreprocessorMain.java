package eu.modernmt.cli;

import eu.modernmt.engine.training.preprocessing.FilesCorporaPartition;
import eu.modernmt.engine.training.preprocessing.Preprocessor;
import eu.modernmt.model.ParallelCorpus;
import eu.modernmt.model.ParallelFilesCorpus;
import eu.modernmt.processing.Languages;
import eu.modernmt.processing.detokenizer.moses.MosesDetokenizer;
import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.Splitter;
import eu.modernmt.processing.util.StringJoiner;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 17/12/15.
 */
public class PreprocessorMain {

    public static final int DEFAULT_PARTITION_SIZE = 1200;

    private static final Options cliOptions;

    static {
        Option sourceLanguage = Option.builder("s").hasArg().required().build();
        Option targetLanguage = Option.builder("t").hasArg().required().build();
        Option inputPath = Option.builder().longOpt("input").hasArg().required().build();
        Option outputPath = Option.builder().longOpt("output").hasArg().required().build();
        Option devPath = Option.builder().longOpt("dev").hasArg().required(false).build();
        Option testPath = Option.builder().longOpt("test").hasArg().required(false).build();

        cliOptions = new Options();
        cliOptions.addOption(sourceLanguage);
        cliOptions.addOption(targetLanguage);
        cliOptions.addOption(inputPath);
        cliOptions.addOption(outputPath);
        cliOptions.addOption(devPath);
        cliOptions.addOption(testPath);
    }

    public static void main(String[] args) throws ParseException, IOException, InterruptedException, ProcessingException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cli = parser.parse(cliOptions, args);

        String source = cli.getOptionValue('s');
        String target = cli.getOptionValue('t');
        File inputPath = new File(cli.getOptionValue("input"));
        File outputPath = new File(cli.getOptionValue("output"));

        if (!inputPath.isDirectory())
            throw new ParseException("Input path is not a valid directory");

        List<? extends ParallelCorpus> corpora = ParallelFilesCorpus.list(inputPath, source, target);

        if (corpora.isEmpty())
            throw new ParseException("Input path does not contains valid parallel data");


        FilesCorporaPartition partition = new FilesCorporaPartition(outputPath);
        Preprocessor preprocessor = new Preprocessor(partition, corpora);

        FileUtils.deleteDirectory(outputPath);

        if (cli.hasOption("dev")) {
            File devOutput = new File(cli.getOptionValue("dev"));
            FileUtils.deleteDirectory(devOutput);

            preprocessor.addExtraPartition(new FilesCorporaPartition(devOutput, DEFAULT_PARTITION_SIZE));
        }

        if (cli.hasOption("test")) {
            File testOutput = new File(cli.getOptionValue("test"));
            FileUtils.deleteDirectory(testOutput);

            preprocessor.addExtraPartition(new FilesCorporaPartition(testOutput, DEFAULT_PARTITION_SIZE));
        }

        preprocessor.process();
    }

}
