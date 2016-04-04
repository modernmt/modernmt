package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;
import eu.modernmt.processing.detokenizer.SpaceNormalizer;
import eu.modernmt.processing.framework.*;
import eu.modernmt.processing.framework.string.XMLEditableString;
import eu.modernmt.processing.numbers.NumericWordFactory;
import eu.modernmt.processing.tokenizer.SimpleTokenizer;
import eu.modernmt.processing.tokenizer.Tokenizer;
import eu.modernmt.processing.tokenizer.Tokenizers;
import eu.modernmt.processing.util.RareCharsNormalizer;
import eu.modernmt.processing.util.WhitespacesNormalizer;
import eu.modernmt.processing.xml.XMLStringBuilder;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 19/02/16.
 */
public class Preprocessor implements Closeable {

    private static final XMLStringBuilder XMLStringBuilder;
    private static final RareCharsNormalizer RareCharsNormalizer;
    private static final WhitespacesNormalizer WhitespacesNormalizer;
    private static final SentenceBuilder SentenceBuilder;

    static {
        XMLStringBuilder = new XMLStringBuilder();
        RareCharsNormalizer = new RareCharsNormalizer();
        WhitespacesNormalizer = new WhitespacesNormalizer();

        SentenceBuilder = new SentenceBuilder();
        SentenceBuilder.addWordFactory(NumericWordFactory.class);
    }

    private final ProcessingPipeline<String, Sentence> pipelineWithTokenization;
    private final ProcessingPipeline<String, Sentence> pipelineWithoutTokenization;

    public static ProcessingPipeline<String, Sentence> getPipeline(Locale language, boolean tokenize) {
        return getPipeline(language, tokenize, Runtime.getRuntime().availableProcessors());
    }

    public static ProcessingPipeline<String, Sentence> getPipeline(Locale language, boolean tokenize, int threads) {
        Tokenizer languageTokenizer = tokenize ? Tokenizers.forLanguage(language) : new SimpleTokenizer();
        SpaceNormalizer spaceNormalizer = tokenize ? SpaceNormalizer.forLanguage(language) : null;

        return new ProcessingPipeline.Builder<String, String>()
                .setThreads(threads)
                .add(XMLStringBuilder)
                .add(RareCharsNormalizer)
                .add(WhitespacesNormalizer)
                .add(spaceNormalizer)
                .add(languageTokenizer)
                .add(SentenceBuilder)
                .create();
    }

    public Preprocessor(Locale language) {
        this(language, Runtime.getRuntime().availableProcessors());
    }

    public Preprocessor(Locale language, int threads) {
        pipelineWithTokenization = getPipeline(language, true, threads);
        pipelineWithoutTokenization = getPipeline(language, false, threads);
    }

    public Sentence[] process(List<String> text, boolean tokenize) throws ProcessingException {
        return process(text.toArray(new String[text.size()]), tokenize);
    }

    public Sentence[] process(String[] text, boolean tokenize) throws ProcessingException {
        BatchTask task = new BatchTask(text);
        ProcessingPipeline<String, Sentence> pipeline = tokenize ? pipelineWithTokenization : pipelineWithoutTokenization;

        try {
            ProcessingJob<String, Sentence> job = pipeline.createJob(task, task);
            job.start();
            job.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unexpected exception", e);
        }

        return task.getResult();
    }

    public Sentence process(String text, boolean tokenize) throws ProcessingException {
        if (tokenize)
            return pipelineWithTokenization.process(text);
        else
            return pipelineWithoutTokenization.process(text);
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(pipelineWithTokenization);
        IOUtils.closeQuietly(pipelineWithoutTokenization);
    }

    private static class BatchTask implements PipelineInputStream<String>, PipelineOutputStream<Sentence> {

        private String[] source;
        private Sentence[] result;
        private int readIndex;
        private int writeIndex;

        public BatchTask(String[] source) {
            this.source = source;
            this.result = new Sentence[source.length];
            this.readIndex = 0;
            this.writeIndex = 0;
        }

        @Override
        public String read() {
            if (readIndex < source.length)
                return source[readIndex++];
            else
                return null;
        }

        @Override
        public void write(Sentence value) {
            result[writeIndex++] = value;
        }

        public Sentence[] getResult() {
            return result;
        }

        @Override
        public void close() throws IOException {
        }

    }

    public static void main(String[] args) throws Throwable {
//        XMLEditableString XMLEditableString = new XMLEditableString("&apos;<b><t>That</b> `s\t\t \tit! &apos;&#40;\t\t");
//        XMLEditableString XMLEditableString = new XMLEditableString("That `s\t\t \tit!");
        String string = "&apos;<b><t>That</b> `s\t\t \tit 127 128! &apos;<b>\t <a> <c>&#40;\t\t";

        Locale language = Locale.ENGLISH;
        SentenceBuilder sentenceBuilder = new SentenceBuilder();
        sentenceBuilder.addWordFactory(NumericWordFactory.class);

        XMLEditableString xmlEditableString = new XMLStringBuilder().call(string);
        xmlEditableString = process(new RareCharsNormalizer(), xmlEditableString);
        xmlEditableString = process(new WhitespacesNormalizer(), xmlEditableString);
        xmlEditableString = process(SpaceNormalizer.forLanguage(language), xmlEditableString);
        xmlEditableString = process(Tokenizers.forLanguage(language), xmlEditableString);
        Sentence sentence = sentenceBuilder.call(xmlEditableString);

        System.out.println(string);
        System.out.println(sentence.toString(false));

        for (Token token : sentence)
            System.out.print(token.getText() + ' ');
        System.out.println();
        for (Token token : sentence)
            System.out.print(token.getPlaceholder() + ' ');
        System.out.println();
    }

    private static XMLEditableString process(TextProcessor<XMLEditableString, XMLEditableString> processor, XMLEditableString string) throws ProcessingException {
        String processorName = processor.getClass().getSimpleName();

//        System.out.println("Running " + processorName);
        XMLEditableString result = processor.call(string);

//        System.out.println(processorName + ":    \"" + result.toString() + "\"");
//        System.out.println();

        return result;
    }
}
