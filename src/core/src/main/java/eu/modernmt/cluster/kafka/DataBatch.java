package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.DataMessage;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
import eu.modernmt.engine.ContributionOptions;
import eu.modernmt.engine.Engine;
import eu.modernmt.lang.LanguageIndex;
import eu.modernmt.lang.LanguagePair;
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
    private final HashMap<Short, Long> currentPositions = new HashMap<>();

    private final Engine engine;
    private final KafkaDataManager manager;

    private final Stack<DataPartition> cachedPartitions = new Stack<>();
    private final HashMap<LanguagePair, DataPartition> cachedDataSet = new HashMap<>();

    public DataBatch(Engine engine, KafkaDataManager manager) {
        this.engine = engine;
        this.manager = manager;
    }

    public void clear() {
        translationUnits.clear();
        deletions.clear();
        currentPositions.clear();
    }

    private DataPartition getDataPartition(int expectedSize) {
        if (cachedPartitions.isEmpty())
            return new DataPartition().reset(expectedSize);
        else
            return cachedPartitions.pop().reset(expectedSize);
    }

    private void releaseDataPartition(DataPartition partition) {
        cachedPartitions.push(partition.clear());
    }

    public void load(ConsumerRecords<Integer, KafkaPacket> records) throws ProcessingException, AlignerException {
        LanguageIndex languages = engine.getLanguages();

        this.clear();

        int size = records.count();

        this.cachedDataSet.clear();
        for (ConsumerRecord<Integer, KafkaPacket> record : records) {
            KafkaChannel channel = this.manager.getChannel(record.topic());
            long offset = record.offset();

            this.currentPositions.put(channel.getId(), offset);

            KafkaPacket packet = record.value();
            DataMessage message = packet.toDataMessage(channel.getId(), offset);

            if (message instanceof TranslationUnit) {
                TranslationUnit unit = (TranslationUnit) message;

                LanguagePair direction = languages.map(unit.direction);
                if (direction != null) {
                    unit.direction = direction;
                    cachedDataSet.computeIfAbsent(unit.direction, key -> getDataPartition(size))
                            .add(unit);
                }

            } else {
                deletions.add((Deletion) message);
            }
        }

        this.translationUnits.ensureCapacity(size);
        for (DataPartition partition : cachedDataSet.values()) {
            partition.process(engine);
            this.translationUnits.addAll(partition.units);
            releaseDataPartition(partition);
        }

        this.cachedDataSet.clear();
    }

    public Map<Short, Long> getBatchOffset() {
        return currentPositions;
    }

    public int size() {
        return translationUnits.size() + deletions.size();
    }

    public List<TranslationUnit> getTranslationUnits() {
        return translationUnits;
    }

    public Collection<Deletion> getDeletions() {
        return deletions;
    }

    private static class DataPartition {

        public final ArrayList<TranslationUnit> units = new ArrayList<>();
        public final ArrayList<String> sources = new ArrayList<>();
        public final ArrayList<String> targets = new ArrayList<>();

        public DataPartition reset(int size) {
            this.clear();

            units.ensureCapacity(size);
            sources.ensureCapacity(size);
            targets.ensureCapacity(size);

            return this;
        }

        public DataPartition clear() {
            units.clear();
            sources.clear();
            targets.clear();

            return this;
        }

        public void add(TranslationUnit unit) {
            units.add(unit);
            sources.add(unit.rawSentence);
            targets.add(unit.rawTranslation);
        }

        public void process(Engine engine) throws ProcessingException, AlignerException {
            if (units.isEmpty())
                return;

            ContributionOptions options = engine.getContributionOptions();
            LanguagePair direction = units.get(0).direction;

            if (options.process) {
                Preprocessor preprocessor = engine.getPreprocessor();
                List<Sentence> sourceSentences = preprocessor.process(direction, sources);
                List<Sentence> targetSentences = preprocessor.process(direction.reversed(), targets);
                Alignment[] alignments = null;

                if (options.align) {
                    Aligner aligner = engine.getAligner();
                    alignments = aligner.getAlignments(direction, sourceSentences, targetSentences);
                }

                for (int i = 0; i < units.size(); i++) {
                    TranslationUnit unit = units.get(i);
                    unit.sentence = sourceSentences.get(i);
                    unit.translation = targetSentences.get(i);

                    if (alignments != null)
                        unit.alignment = alignments[i];
                }
            }
        }
    }
}
