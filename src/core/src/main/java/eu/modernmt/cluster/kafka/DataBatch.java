package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.DataManager;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.engine.Engine;
import eu.modernmt.model.Alignment;
import eu.modernmt.model.Sentence;
import eu.modernmt.processing.Preprocessor;
import eu.modernmt.processing.ProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by davide on 06/09/16.
 */
class DataBatch implements Iterable<TranslationUnit> {

    private final Logger logger = LogManager.getLogger(KafkaDataManager.class);

    private final ArrayList<TranslationUnit> data = new ArrayList<>();
    private final ArrayList<String> rawSources = new ArrayList<>();
    private final ArrayList<String> rawTargets = new ArrayList<>();

    private final Aligner aligner;
    private final Preprocessor sourcePreprocessor;
    private final Preprocessor targetPreprocessor;

    private HashMap<Short, Long> currentPositions;

    public DataBatch(Engine engine) {
        this.aligner = engine.getAligner();
        this.sourcePreprocessor = engine.getSourcePreprocessor();
        this.targetPreprocessor = engine.getTargetPreprocessor();
    }

    public void load(ConsumerRecords<Integer, KafkaElement> records) throws ProcessingException, AlignerException {
        int size = records.count();

        this.data.clear();
        this.rawSources.clear();
        this.rawTargets.clear();

        this.data.ensureCapacity(size);
        this.rawSources.ensureCapacity(size);
        this.rawTargets.ensureCapacity(size);

        this.currentPositions = new HashMap<>();

        for (ConsumerRecord<Integer, KafkaElement> record : records) {
            KafkaChannel channel = KafkaDataManager.getChannel(record.topic());
            KafkaElement value = record.value();
            long offset = record.offset();

            TranslationUnit unit = value.toTranslationUnit(channel.getId(), offset);

            data.add(unit);
            rawSources.add(value.getSourceSentence());
            rawTargets.add(value.getTargetSentence());

            this.currentPositions.put(channel.getId(), offset);
        }

        List<Sentence> sourceSentences = sourcePreprocessor.process(rawSources, true);
        List<Sentence> targetSentences = targetPreprocessor.process(rawTargets, true);

        Alignment[] alignments = aligner.getAlignments(sourceSentences, targetSentences);

        for (int i = 0; i < alignments.length; i++) {
            TranslationUnit unit = data.get(i);
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
        return data.size();
    }

    public void clear() {
        this.data.clear();
    }

    @Override
    public Iterator<TranslationUnit> iterator() {
        return data.iterator();
    }

}
