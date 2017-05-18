package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.DataMessage;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import java.util.*;

/**
 * Created by davide on 06/09/16.
 */
class DataBatch {

    private final ArrayList<TranslationUnit> translationUnits = new ArrayList<>();
    private final ArrayList<Deletion> deletions = new ArrayList<>();
    private final ArrayList<String> rawSources = new ArrayList<>();
    private final ArrayList<String> rawTargets = new ArrayList<>();

    private final Aligner aligner;
    private final Preprocessor sourcePreprocessor;
    private final Preprocessor targetPreprocessor;

    private KafkaDataManager manager;

    private HashMap<Short, Long> currentPositions;

    public DataBatch(Engine engine, KafkaDataManager manager) {
        this.aligner = engine.getAligner();
        this.sourcePreprocessor = engine.getSourcePreprocessor();
        this.targetPreprocessor = engine.getTargetPreprocessor();
        this.manager = manager;
    }

    public void load(ConsumerRecords<Integer, KafkaElement> records) throws ProcessingException, AlignerException {
        this.clear();
        this.ensureCapacity(records.count());

        this.currentPositions = new HashMap<>();

        for (ConsumerRecord<Integer, KafkaElement> record : records) {
            KafkaChannel channel = this.manager.getChannel(record.topic());
            KafkaElement value = record.value();
            long offset = record.offset();

            DataMessage message = value.toDataMessage(channel.getId(), offset);

            if (message instanceof TranslationUnit) {
                TranslationUnit unit = (TranslationUnit) message;
                translationUnits.add(unit);
                rawSources.add(unit.originalSourceSentence);
                rawTargets.add(unit.originalTargetSentence);
            } else {
                deletions.add((Deletion) message);
            }

            this.currentPositions.put(channel.getId(), offset);
        }

        List<Sentence> sourceSentences = sourcePreprocessor.process(rawSources);
        List<Sentence> targetSentences = targetPreprocessor.process(rawTargets);

        Alignment[] alignments = aligner.getAlignments(sourceSentences, targetSentences);

        for (int i = 0; i < alignments.length; i++) {
            TranslationUnit unit = translationUnits.get(i);
            unit.sourceSentence = sourceSentences.get(i);
            unit.targetSentence = targetSentences.get(i);
            unit.alignment = alignments[i];
        }

        this.rawSources.clear();
        this.rawTargets.clear();
    }

    public Map<Short, Long> getBatchOffset() {
        return currentPositions;
    }

    public int size() {
        return translationUnits.size() + deletions.size();
    }

    public void clear() {
        this.translationUnits.clear();
        this.deletions.clear();
        this.rawSources.clear();
        this.rawTargets.clear();
    }

    public void ensureCapacity(int size) {
        this.translationUnits.ensureCapacity(size);
        this.rawSources.ensureCapacity(size);
        this.rawTargets.ensureCapacity(size);
    }

    public Collection<TranslationUnit> getTranslationUnits() {
        return translationUnits;
    }

    public Collection<Deletion> getDeletions() {
        return deletions;
    }

}
