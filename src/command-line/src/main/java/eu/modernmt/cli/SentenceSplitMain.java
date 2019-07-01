package eu.modernmt.cli;

import eu.modernmt.io.LineReader;
import eu.modernmt.io.UTF8Charset;
import eu.modernmt.io.UnixLineReader;
import eu.modernmt.io.UnixLineWriter;
import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.splitter.SentenceSplitter;
import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;

/**
 * Created by davide on 11/12/17.
 */
public class SentenceSplitMain {

    private static class Args {

        private static final Options cliOptions;

        static {
            Option language = Option.builder("l").hasArg().required().build();

            cliOptions = new Options();
            cliOptions.addOption(language);
        }

        public final LanguageDirection language;

        public Args(String[] args) throws ParseException {
            CommandLineParser parser = new DefaultParser();
            CommandLine cli = parser.parse(cliOptions, args);

            Language2 lang = Language2.fromString(cli.getOptionValue("l"));
            language = new LanguageDirection(lang, lang);
        }

    }

    public static void main(String[] _args) throws Throwable {
        Args args = new Args(_args);

        Preprocessor preprocessor = null;
        SentenceOutputter output = null;

        LineReader input = new UnixLineReader(System.in, UTF8Charset.get());

        try {
            preprocessor = new Preprocessor();
            output = new SentenceOutputter(args.language.source);
            output.start();

            String[] batch = new String[preprocessor.getThreads()];
            int i = 0;

            String line;
            while ((line = input.readLine()) != null) {
                if (i == batch.length) {
                    Sentence[] sentences = preprocessor.process(args.language, batch);
                    output.write(sentences);

                    i = 0;
                }

                batch[i++] = line;
            }

            if (i > 0) {
                String[] copy = new String[i];
                System.arraycopy(batch, 0, copy, 0, i);

                Sentence[] sentences = preprocessor.process(args.language, copy);
                output.write(sentences);
            }
        } finally {
            IOUtils.closeQuietly(preprocessor);
            IOUtils.closeQuietly(output);
        }
    }

    private static class SentenceOutputter extends Thread implements Closeable {

        private final SentenceSplitter splitter;
        private final UnixLineWriter writer;
        private final SynchronousQueue<Sentence[]> job = new SynchronousQueue<>();

        public SentenceOutputter(Language2 language) {
            this.writer = new UnixLineWriter(System.out, UTF8Charset.get());
            this.splitter = SentenceSplitter.forLanguage(language);
        }

        public void write(Sentence[] batch) throws InterruptedException {
            job.put(batch);
        }

        private Sentence[] take() {
            try {
                Sentence[] batch = job.take();
                if (batch == null || batch.length == 0)
                    return null;
                return batch;
            } catch (InterruptedException e) {
                return null;
            }
        }

        @Override
        public void run() {
            Sentence[] batch;
            while ((batch = take()) != null) {
                for (Sentence sentence : batch) {
                    for (Sentence subSentence : splitter.split(sentence)) {
                        try {
                            writer.writeLine(subSentence.toString(false, false));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        @Override
        public void close() throws IOException {
            try {
                job.put(new Sentence[0]);
                this.join();
            } catch (InterruptedException e) {
                throw new IOException(e);
            }

            writer.close();
        }
    }

}
