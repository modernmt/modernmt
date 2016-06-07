package eu.modernmt.core.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.map.listener.EntryRemovedListener;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by davide on 21/04/16.
 */
public class SessionManager {

    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    private final IMap<Long, TranslationSession> sessions;
    private final IdGenerator idGenerator;

    SessionManager(HazelcastInstance hazelcast, EntryRemovedListener<Long, TranslationSession> listener) {
        this.idGenerator = hazelcast.getIdGenerator(ClusterConstants.TRANSLATION_SESSION_ID_GENERATOR_NAME);
        this.sessions = hazelcast.getMap(ClusterConstants.TRANSLATION_SESSION_MAP_NAME);
        this.sessions.addEntryListener(listener, true);
    }

    public TranslationSession get(long id) {
        return sessions.get(id);
    }

    public TranslationSession create(List<ContextDocument> context) {
        long id = idGenerator.newId() + 1; // starts from 0
        TranslationSession session = new TranslationSessionImpl(this.sessions, id, context);
        sessions.put(id, session);
        return session;
    }

    private static class TranslationSessionImpl extends TranslationSession {

        private transient Map<Long, TranslationSession> sessionMap;

        private TranslationSessionImpl(Map<Long, TranslationSession> sessionMap, long id, List<ContextDocument> translationContext) {
            super(id, translationContext);
            this.sessionMap = sessionMap;
        }

        @Override
        public void close() {
            sessionMap.remove(this.id);
        }
    }

}
