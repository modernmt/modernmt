package eu.modernmt.rest.actions.util;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.Parameters;

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
                ids.add(e.domain.getId());
            }
        }

        Map<Long, Domain> domains = ModernMT.domain.get(ids);

        for (ContextVector context : collection) {
            for (ContextVector.Entry e : context) {
                copy(domains.get(e.domain.getId()), e.domain);
            }
        }
    }

    private static void copy(Domain from, Domain to) {
        to.setName(from.getName());
    }

    public static ContextVector parseParameter(String name, String value) throws Parameters.ParameterParsingException {
        String[] elements = value.split(",");

        ContextVector.Builder builder = new ContextVector.Builder(elements.length);

        for (String element : elements) {
            String[] keyvalue = element.split(":");

            if (keyvalue.length != 2)
                throw new Parameters.ParameterParsingException(name, value);

            long domainId;
            float score;

            try {
                domainId = Long.parseLong(keyvalue[0]);
                score = Float.parseFloat(keyvalue[1]);
            } catch (NumberFormatException e) {
                throw new Parameters.ParameterParsingException(name, value);
            }

            if (domainId < 1)
                throw new Parameters.ParameterParsingException(name, value);

            if (score < 0.f || score > 1.f)
                throw new Parameters.ParameterParsingException(name, value);

            builder.add(domainId, score);
        }

        return builder.build();
    }

}
