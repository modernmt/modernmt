package eu.modernmt.datastream;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by davide on 06/09/16.
 */
class UpdateBatch implements Iterable<Update> {

    private final ArrayList<Update> data = new ArrayList<>();
    private final ArrayList<String> sources = new ArrayList<>();
    private final ArrayList<String> targets = new ArrayList<>();

    private final Aligner aligner;
    private final Preprocessor sourcePreprocessor;
    private final Preprocessor targetPreprocessor;

    public UpdateBatch(Engine engine) {
        this.aligner = engine.getAligner();
        this.sourcePreprocessor = engine.getSourcePreprocessor();
        this.targetPreprocessor = engine.getTargetPreprocessor();
    }

    private static int getTopicId(String topic) {
        for (int i = 0; i < DataStreamManager.TOPICS.length; i++) {
            if (DataStreamManager.TOPICS[i].equals(topic))
                return i;
        }

        throw new IllegalArgumentException("Invalid topic name: " + topic);
    }

    public void load(ConsumerRecords<Integer, StreamUpdate> records) throws ProcessingException, AlignerException {
        int size = records.count();

        this.data.clear();
        this.sources.clear();
        this.targets.clear();

        this.data.ensureCapacity(size);
        this.sources.ensureCapacity(size);
        this.targets.ensureCapacity(size);

        for (ConsumerRecord<Integer, StreamUpdate> record : records) {
            StreamUpdate value = record.value();
            Update update = Update.fromStream(getTopicId(record.topic()), record.offset(), value);

            data.add(update);
            sources.add(value.getSourceSentence());
            targets.add(value.getTargetSentence());
        }

        List<Sentence> sourceSentences = sourcePreprocessor.process(sources);
        List<Sentence> targetSentences = targetPreprocessor.process(targets);

        Alignment[] alignments = aligner.getAlignments(sourceSentences, targetSentences);

        for (int i = 0; i < alignments.length; i++) {
            Update update = data.get(i);
            update.sourceSentence = sourceSentences.get(i);
            update.targetSentence = targetSentences.get(i);
            update.alignment = alignments[i];
        }

        this.sources.clear();
        this.targets.clear();
    }

    public int size() {
        return data.size();
    }

    public void clear() {
        this.data.clear();
    }

    @Override
    public Iterator<Update> iterator() {
        return data.iterator();
    }

}
