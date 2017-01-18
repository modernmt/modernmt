package eu.modernmt.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.map.listener.EntryRemovedListener;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.model.ContextVector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Created by davide on 21/04/16.
 */
public class SessionManager {

    private static final Logger logger = LogManager.getLogger(SessionManager.class);

    private final IMap<Long, TranslationSessionImpl> sessions;
    private final IdGenerator idGenerator;

    SessionManager(HazelcastInstance hazelcast, EntryRemovedListener<Long, TranslationSession> listener) {
        this.idGenerator = hazelcast.getIdGenerator(ClusterConstants.TRANSLATION_SESSION_ID_GENERATOR_NAME);
        this.sessions = hazelcast.getMap(ClusterConstants.TRANSLATION_SESSION_MAP_NAME);
        this.sessions.addEntryListener(listener, true);
    }

    public TranslationSession get(long id) {
        TranslationSessionImpl session = sessions.get(id);
        if (session != null)
            session.sessionMap = this.sessions;
        return session;
    }

    public TranslationSession create(ContextVector contextVector) {
        long id = idGenerator.newId() + 1; // starts from 0
        TranslationSessionImpl session = new TranslationSessionImpl(id, contextVector);
        session.sessionMap = this.sessions;
        sessions.put(id, session);
        return session;
    }

    private static class TranslationSessionImpl extends TranslationSession {

        transient Map<Long, TranslationSessionImpl> sessionMap;

        private TranslationSessionImpl(long id, ContextVector contextVector) {
            super(id, contextVector);
        }

        @Override
        public void close() {
            sessionMap.remove(this.id);
        }
    }

}
