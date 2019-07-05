package eu.modernmt.api.actions.util;

import eu.modernmt.api.framework.Parameters;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Memory;
import eu.modernmt.persistence.PersistenceException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by davide on 20/10/16.
 */
public class ContextUtils {

    public static void resolve(ContextVector context) throws PersistenceException {
        resolve(Collections.singleton(context));
    }

    public static void resolve(Collection<ContextVector> collection) throws PersistenceException {
        HashSet<Long> ids = new HashSet<>();
        for (ContextVector context : collection) {
            for (ContextVector.Entry e : context) {
                ids.add(e.memory.getId());
            }
        }

        Map<Long, Memory> memories = ModernMT.memory.get(ids);

        for (ContextVector context : collection) {
            for (ContextVector.Entry e : context) {
                copy(memories.get(e.memory.getId()), e.memory);
            }
        }
    }

    private static void copy(Memory from, Memory to) {
        if (from == null)
            return;

        to.setOwner(from.getOwner());
        to.setName(from.getName());
    }

    public static ContextVector parseParameter(String name, String value) throws Parameters.ParameterParsingException {
        try {
            return ContextVector.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new Parameters.ParameterParsingException(name, value);
        }
    }

}
