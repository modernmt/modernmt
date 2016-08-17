package eu.modernmt.training;

import eu.modernmt.io.LineWriter;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.Word;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingException;
import eu.modernmt.processing.util.TokensOutputter;
import eu.modernmt.training.partitioning.CorporaPartition;
import eu.modernmt.training.partitioning.PartitionWriter;
import eu.modernmt.training.partitioning.PartitionedInputStream;
import eu.modernmt.vocabulary.VocabularyBuilder;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by davide on 24/02/16.
 */
class TrainingCorpusTask implements Callable<Void> {

    private Preprocessor preprocessor;

    private Corpus corpus;
    private int corpusLines;

    private CorporaPartition mainPartition;
    private List<PartitionWriter> extraPartitions = new ArrayList<>();
    private VocabularyBuilder vocabularyBuilder;

    public TrainingCorpusTask(Preprocessor preprocessor, Corpus corpus, int corpusLines, CorporaPartition mainPartition) {
        this.preprocessor = preprocessor;
        this.corpus = corpus;
        this.corpusLines = corpusLines;
        this.mainPartition = mainPartition;
    }

    public void setVocabularyBuilder(VocabularyBuilder vocabularyBuilder) {
        this.vocabularyBuilder = vocabularyBuilder;
    }

    public void addExtraPartition(CorporaPartition partition, int size) {
        extraPartitions.add(new PartitionWriter(partition, corpus, size));
    }

    @Override
    public Void call() throws ProcessingException, InterruptedException {
        PipelineInputStream<String> input = null;
        PipelineOutputStream<Sentence> output = null;

        try {
            // Input
            if (extraPartitions.size() > 0)
                input = new PartitionedInputStream(corpus, corpusLines, extraPartitions);
            else
                input = PipelineInputStream.fromLineReader(corpus.getContentReader());

            // Output
            Corpus outCorpus = mainPartition.getDestinationCorpus(this.corpus);
            LineWriter writer = outCorpus.getContentWriter(false);

            output = vocabularyBuilder == null ? new TokensOutputter(writer, false, true) : new IdsOutputter(writer, vocabularyBuilder);

            // Process
            preprocessor.process(input, output, true);
        } catch (IOException | ProcessingException e) {
            throw new ProcessingException("Failed to process corpus '" + corpus.getName() + "'", e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);

            extraPartitions.forEach(IOUtils::closeQuietly);
        }

        return null;
    }

    private static class IdsOutputter implements PipelineOutputStream<Sentence> {

        private static final int DEFAULT_BUFFER_SIZE_IN_WORDS = 1000000;

        private final LineWriter writer;
        private final VocabularyBuilder vocabularyBuilder;

        private ArrayList<String[]> buffer = new ArrayList<>(DEFAULT_BUFFER_SIZE_IN_WORDS / 10);
        private final int maxBufferSizeInWords = DEFAULT_BUFFER_SIZE_IN_WORDS;
        private int bufferSizeInWords = 0;

        public IdsOutputter(LineWriter writer, VocabularyBuilder vocabularyBuilder) {
            this.writer = writer;
            this.vocabularyBuilder = vocabularyBuilder;
        }

        @Override
        public void close() throws IOException {
            this.flush();
            writer.close();
        }

        @Override
        public void write(Sentence sentence) throws IOException {
            if (bufferSizeInWords >= maxBufferSizeInWords)
                this.flush();

            Word[] words = sentence.getWords();
            String[] line = new String[words.length];
            for (int i = 0; i < line.length; i++)
                line[i] = words[i].getPlaceholder();

            buffer.add(line);
            bufferSizeInWords += line.length;
        }

        private void flush() throws IOException {
            if (buffer.isEmpty())
                return;

            String[][] arrayBuffer = buffer.toArray(new String[buffer.size()][]);
            bufferSizeInWords = 0;
            buffer.clear();

            int[][] output = vocabularyBuilder.addLines(arrayBuffer);

            StringBuilder builder = new StringBuilder();
            for (int[] line : output) {
                for (int i = 0; i < line.length; i++) {
                    if (i > 0)
                        builder.append(' ');
                    builder.append(line[i]);
                }

                writer.writeLine(builder.toString());
                builder.setLength(0);
            }
        }

    }
}
