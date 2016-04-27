package eu.modernmt.core.cluster;

/**
 * Created by davide on 20/04/16.
 */
public class ClusterConstants {

    public static final int DEFAULT_TRANSLATION_EXECUTOR_SIZE;
    public static final String TRANSLATION_EXECUTOR_NAME = "TranslationsExecutor";
    public static final String TRANSLATION_SESSION_MAP_NAME = "TranslationSessionMap";
    public static final String TRANSLATION_SESSION_ID_GENERATOR_NAME = "TranslationSessionIdGenerator";
    public static final String DECODER_WEIGHTS_TOPIC_NAME = "DecoderWeightsTopic";
    public static final String MEMBERS_MODEL_PATH_MAP_NAME = "MembersModelPathMap";

    static {
        int cores = Runtime.getRuntime().availableProcessors();

        // Accordingly to "Fast, Scalable Phrase-Based SMT Decoding" [ACL 2016 Submission]
        // current version of Moses Decoder seems to not scale well if number of threads is
        // more than 16. Further tests showed that best trade-off is 14
        cores = Math.min(14, cores);

        DEFAULT_TRANSLATION_EXECUTOR_SIZE = cores;
    }

}
