


package eu.modernmt.cli;

import eu.modernmt.aligner.fastalign.FastAlign;
import eu.modernmt.cli.log4j.Log4jConfiguration;
import eu.modernmt.io.LineReader;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.impl.parallel.FileCorpus;
import eu.modernmt.processing.Preprocessor;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by andrea on 07/08/17.
 */
public class AlignTestMain {
    private static class Args {
        private static final Options cliOptions;

        static {
            Option modelPath = Option.builder("m").longOpt("model").hasArg().required().build();
            Option sourcePath = Option.builder("s").longOpt("source").hasArg().required().build();
            Option targetPath = Option.builder("t").longOpt("target").hasArg().required().build();
            Option outputPath = Option.builder("o").longOpt("output").hasArg().required().build();
            cliOptions = new Options();
            cliOptions.addOption(modelPath);
            cliOptions.addOption(sourcePath);
            cliOptions.addOption(targetPath);
            cliOptions.addOption(outputPath);
        }

        public final File model;
        public final File sourceFile;
        public final File targetFile;
        public final File outputFile;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);
            model = new File(cli.getOptionValue('m'));
            sourceFile = new File(cli.getOptionValue('s'));
            targetFile = new File(cli.getOptionValue('t'));
            outputFile = new File(cli.getOptionValue('o'));
        }
    }

    public static void main(String[] _args) throws Throwable {
        Log4jConfiguration.setup(Level.INFO);
        Args args = new Args(_args);

        FileCorpus srcCorpus = new FileCorpus(args.sourceFile);
        FileCorpus trgCorpus = new FileCorpus(args.targetFile);
        Locale source = srcCorpus.getLanguage();
        Locale target = trgCorpus.getLanguage();
        LineReader srcReader = null;
        LineReader trgReader = null;
        FileWriter writer = null;
        LanguagePair direction;
        direction = new LanguagePair(source, target);
        System.out.println("Original direction: " + direction);
        if (source.toLanguageTag().compareTo(target.toLanguageTag()) > 0)
            System.out.println("Print direction: " + target.toLanguageTag() + "->" + source.toLanguageTag());
        else
            System.out.println("Print direction: " + source.toLanguageTag() + "->" + target.toLanguageTag());
        Preprocessor preprocessor = new Preprocessor();
        try {
            FileUtils.touch(args.outputFile);
            srcReader = srcCorpus.getContentReader();
            trgReader = trgCorpus.getContentReader();
            writer = new FileWriter(args.outputFile);
            FastAlign aligner = new FastAlign(args.model);

            String sourceString = srcReader.readLine();
            String targetString = trgReader.readLine();
            Sentence srcSentence;
            Sentence trgSentence;
            Alignment alignment;
            while (sourceString != null && targetString != null) {
                srcSentence = preprocessor.process(direction, sourceString);
                trgSentence = preprocessor.process(direction.reversed(), targetString);
                alignment = aligner.getAlignment(direction, srcSentence, trgSentence);
                if (source.toLanguageTag().compareTo(target.toLanguageTag()) > 0)
                    alignment = alignment.getInverse();
                else
                    alignment = alignment.getInverse().getInverse();

                writer.write(alignment.toString() + "\n");
                sourceString = srcReader.readLine();
                targetString = trgReader.readLine();
            }
            if (sourceString == null && targetString != null)
                throw new IOException("Source corpus and target corpus have different line amounts");
            else if (sourceString != null && targetString == null)
                throw new IOException("Source corpus and target corpus have different line amounts");
        } finally {
            IOUtils.closeQuietly(srcReader);
            IOUtils.closeQuietly(trgReader);
            IOUtils.closeQuietly(writer);
        }
    }
}