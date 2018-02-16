package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.DataBatch;
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
class KafkaDataBatch implements DataBatch {

    private final ArrayList<TranslationUnit> translationUnits = new ArrayList<>();
    private final ArrayList<Deletion> deletions = new ArrayList<>();
    private final HashMap<Short, Long> currentPositions = new HashMap<>();

    private final Engine engine;
    private final KafkaDataManager manager;

    private final Stack<DataPartition> cachedPartitions = new Stack<>();
    private final HashMap<LanguagePair, DataPartition> cachedDataSet = new HashMap<>();

    public KafkaDataBatch(Engine engine, KafkaDataManager manager) {
        this.engine = engine;
        this.manager = manager;
    }

    public void clear() {
        translationUnits.clear();
        deletions.clear();
        currentPositions.clear();
    }

    private DataPartition getDataPartition(LanguagePair direction, int expectedSize) {
        if (cachedPartitions.isEmpty())
            return new DataPartition().reset(direction, expectedSize);
        else
            return cachedPartitions.pop().reset(direction, expectedSize);
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
            short channelId = channel.getId();

            Long previousOffset = this.currentPositions.get(channelId);
            if (previousOffset == null || previousOffset < offset)
                this.currentPositions.put(channelId, offset);

            KafkaPacket packet = record.value();
            DataMessage message = packet.toDataMessage(channel.getId(), offset);

            if (message instanceof TranslationUnit) {
                TranslationUnit unit = (TranslationUnit) message;

                LanguagePair mapping = languages.mapToBestMatching(unit.direction);

                if (mapping != null) {
                    DataPartition partition = cachedDataSet.computeIfAbsent(mapping, key -> getDataPartition(key, size));
                    partition.add(unit);
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

    public int size() {
        return translationUnits.size() + deletions.size();
    }

    @Override
    public Collection<TranslationUnit> getTranslationUnits() {
        return translationUnits;
    }

    @Override
    public Collection<Deletion> getDeletions() {
        return deletions;
    }

    @Override
    public Map<Short, Long> getChannelPositions() {
        return currentPositions;
    }

    private static class DataPartition {

        private LanguagePair direction;
        public final ArrayList<TranslationUnit> units = new ArrayList<>();
        public final ArrayList<String> sources = new ArrayList<>();
        public final ArrayList<String> targets = new ArrayList<>();

        public DataPartition reset(LanguagePair direction, int size) {
            this.clear();
            this.direction = direction;

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
