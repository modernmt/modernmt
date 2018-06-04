package eu.modernmt.cluster.kafka;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.data.DataBatch;
import eu.modernmt.data.DataMessage;
import eu.modernmt.data.Deletion;
import eu.modernmt.data.TranslationUnit;
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

    public void load(ConsumerRecords<Integer, KafkaPacket> records, boolean process, boolean align) throws ProcessingException, AlignerException {
        LanguageIndex languageIndex = engine.getLanguageIndex();

        // Load records

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
            byte packetType = packet.getType();

            if (packetType == KafkaPacket.TYPE_DELETION) {
                deletions.add(packet.asDeletion());
            } else {
                LanguagePair direction = languageIndex.mapIgnoringDirection(packet.getDirection());
                if (direction != null) {
                    DataPartition partition = cachedDataSet.computeIfAbsent(direction, key -> getDataPartition(key, size));
                    partition.add(packet);
                }
            }
        }

        // Process translation units

        this.translationUnits.ensureCapacity(size);
        for (DataPartition partition : cachedDataSet.values()) {
            partition.process(engine, process, align, this.translationUnits);
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
        public final ArrayList<KafkaPacket> packets = new ArrayList<>();
        public final ArrayList<String> sources = new ArrayList<>();
        public final ArrayList<String> targets = new ArrayList<>();

        public DataPartition reset(LanguagePair direction, int size) {
            this.clear();
            this.direction = direction;

            packets.ensureCapacity(size);
            sources.ensureCapacity(size);
            targets.ensureCapacity(size);

            return this;
        }

        public DataPartition clear() {
            packets.clear();
            sources.clear();
            targets.clear();

            return this;
        }

        public void add(KafkaPacket packet) {
            packets.add(packet);
            sources.add(packet.getSentence());
            targets.add(packet.getTranslation());
        }

        public void process(Engine engine, boolean process, boolean align, Collection<TranslationUnit> output) throws ProcessingException, AlignerException {
            if (packets.isEmpty())
                return;

            if (process || align) {
                Preprocessor preprocessor = engine.getPreprocessor();
                List<Sentence> sourceSentences = preprocessor.process(direction, sources);
                List<Sentence> targetSentences = preprocessor.process(direction.reversed(), targets);
                Alignment[] alignments = null;

                if (align) {
                    Aligner aligner = engine.getAligner();
                    alignments = aligner.getAlignments(direction, sourceSentences, targetSentences);
                }

                for (int i = 0; i < packets.size(); i++) {
                    Sentence sentence = sourceSentences.get(i);
                    Sentence translation = targetSentences.get(i);
                    Alignment alignment = alignments != null ? alignments[i] : null;

                    output.add(packets.get(i).asTranslationUnit(direction, sentence, translation, alignment));
                }
            }
        }
    }
}
