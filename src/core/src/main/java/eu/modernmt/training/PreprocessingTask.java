package eu.modernmt.training;

import eu.modernmt.io.BufferedLineReader;
import eu.modernmt.io.LineReader;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Sentence;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.training.partitioning.CorporaPartition;
import eu.modernmt.training.partitioning.PartitionWriter;
import eu.modernmt.training.partitioning.PartitionedLineReader;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 22/08/16.
 */
class PreprocessingTask {

    private final LanguagePair language;
    private final Corpus corpus;
    private final int corpusLines;

    private final CorporaPartition mainPartition;
    private final List<PartitionWriter> extraPartitions = new ArrayList<>();

    private final Preprocessor preprocessor;

    public PreprocessingTask(Preprocessor preprocessor, LanguagePair language, Corpus corpus, CorporaPartition mainPartition) {
        this(preprocessor, language, corpus, 0, mainPartition);
    }

    public PreprocessingTask(Preprocessor preprocessor, LanguagePair language, Corpus corpus, int lineCount, CorporaPartition mainPartition) {
        this.language = language;
        this.corpus = corpus;
        this.corpusLines = lineCount;
        this.mainPartition = mainPartition;
        this.preprocessor = preprocessor;
    }

    public void addExtraPartition(CorporaPartition partition, int size) {
        extraPartitions.add(new PartitionWriter(partition, corpus, size));
    }

    public void execute() throws ProcessingException, IOException {
        LineReader reader = null;
        AsyncCorpusWriter writer = null;

        try {
            // Input
            reader = corpus.getContentReader();
            if (extraPartitions.size() > 0)
                reader = new PartitionedLineReader(corpus, corpusLines, extraPartitions);

            BufferedLineReader bufferedReader = new BufferedLineReader(reader);
            reader = bufferedReader;

            // Output
            Corpus outCorpus = mainPartition.getDestinationCorpus(this.corpus);
            writer = new AsyncCorpusWriter(outCorpus);

            // Processing
            String[] batch;
            while ((batch = bufferedReader.readLines()) != null) {
                Sentence[] tokenized = preprocessor.process(language, batch);
                writer.write(tokenized);
            }
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);

            extraPartitions.forEach(IOUtils::closeQuietly);
        }
    }


}
