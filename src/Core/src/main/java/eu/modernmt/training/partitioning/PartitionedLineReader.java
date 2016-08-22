package eu.modernmt.training.partitioning;

import eu.modernmt.io.LineReader;
import eu.modernmt.model.corpus.Corpus;
import eu.modernmt.processing.framework.PipelineInputStream;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 12/02/16.
 */
public class PartitionedLineReader implements LineReader {

    private List<PartitionWriter> partitions;
    private LineReader reader;

    private int windowSize;
    private int lineIndex;
    private int partitionIndex;

    public PartitionedLineReader(Corpus corpus, int lines, List<PartitionWriter> partitions) throws IOException {
        this.reader = corpus.getContentReader();
        this.partitions = partitions;

        int extraLines = 0;
        for (PartitionWriter partition : partitions)
            extraLines += partition.size();

        this.windowSize = extraLines > 0 ? lines / extraLines : Integer.MAX_VALUE;
        this.lineIndex = 0;
        this.partitionIndex = 0;
    }

    private String getLine() throws IOException {
        lineIndex++;
        return reader.readLine();
    }

    private void writeToPartition(String line) throws IOException {
        for (int i = 0; i < partitions.size(); i++) {
            partitionIndex = (partitionIndex + 1) % partitions.size();

            PartitionWriter partition = partitions.get(partitionIndex);
            if (partition.write(line))
                break;
        }
    }

    @Override
    public String readLine() throws IOException {
        String line;

        while ((line = getLine()) != null && (lineIndex % windowSize == 0)) {
            writeToPartition(line);
        }

        return line;
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

}
