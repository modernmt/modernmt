package eu.modernmt.core.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IdGenerator;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by davide on 21/04/16.
 */
public class SessionManager {

    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    private final ConcurrentMap<Long, TranslationSession> sessions;
    private final IdGenerator idGenerator;

    SessionManager(HazelcastInstance hazelcast) {
        this.sessions = hazelcast.getMap(ClusterConstants.TRANSLATION_SESSION_MAP_NAME);
        this.idGenerator = hazelcast.getIdGenerator(ClusterConstants.TRANSLATION_SESSION_ID_GENERATOR_NAME);
    }


    public TranslationSession get(long id) {
        return sessions.get(id);
    }

    public TranslationSession create(List<ContextDocument> context) {
        long id = idGenerator.newId();
        TranslationSession session = new TranslationSession(id, context);
        sessions.put(id, session);
        return session;
    }

    public void close(long id) {
        TranslationSession session = sessions.remove(id);
        if (session == null)
            logger.warn("Failed to close session. Session not found: " + id);

        // TODO: should broadcast to members to close their Moses Sessions
    }
}
