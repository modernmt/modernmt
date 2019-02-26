package eu.modernmt.cli;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;
import eu.modernmt.io.*;
import org.apache.commons.cli.*;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class StandfordSplitter {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option minChars = Option.builder("c").hasArg().required(false).build();

            cliOptions = new Options();
            cliOptions.addOption(minChars);
        }

        public final int minChars;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            minChars = Integer.parseInt(cli.getOptionValue("c", "256"));
        }

    }

    public static void split(String string, List<String> output) {
        // Tokenize
        List<CoreLabel> tokens = new ArrayList<>();
        PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(new StringReader(string), new CoreLabelTokenFactory(), "");
        while (tokenizer.hasNext()) {
            tokens.add(tokenizer.next());
        }

        // Get sentences
        List<List<CoreLabel>> sentences = new WordToSentenceProcessor<CoreLabel>().process(tokens);

        // Reconstruct sentences
        int end;
        int start = 0;

        for (List<CoreLabel> sentence : sentences) {
            end = sentence.get(sentence.size() - 1).endPosition();
            output.add(string.substring(start, end).trim());
            start = end;
        }
    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        LineReader input = new UnixLineReader(System.in, UTF8Charset.get());
        LineWriter output = new UnixLineWriter(System.out, UTF8Charset.get());
        ArrayList<String> strings = new ArrayList<>();

        String line;
        while ((line = input.readLine()) != null) {
            line = line.trim();

            if (line.length() > 0) {
                if (line.length() <= args.minChars) {
                    output.writeLine(line);
                } else {
                    strings.clear();
                    split(line, strings);
                    for (String string : strings)
                        output.writeLine(string);
                }
            }
        }
    }

}