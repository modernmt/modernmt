package eu.modernmt.rest.actions.util;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.Parameters;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by davide on 20/10/16.
 */
public class ContextUtils {

    public static ContextVector resolve(ContextVector context) throws PersistenceException {
        ArrayList<Integer> ids = new ArrayList<>(context.size());
        for (ContextVector.Entry e : context)
            ids.add(e.domain.getId());

        Map<Integer, Domain> domains = ModernMT.domain.get(ids);

        ContextVector.Builder builder = new ContextVector.Builder(context.size());
        for (ContextVector.Entry e : context) {
            int id = e.domain.getId();
            builder.add(domains.get(id), e.score);
        }

        return builder.build();
    }

    public static ContextVector parseParameter(String name, String value) throws Parameters.ParameterParsingException {
        String[] elements = value.split(",");

        ContextVector.Builder builder = new ContextVector.Builder(elements.length);

        for (String element : elements) {
            String[] keyvalue = element.split(":");

            if (keyvalue.length != 2)
                throw new Parameters.ParameterParsingException(name, value);

            int domainId;
            float score;

            try {
                domainId = Integer.parseInt(keyvalue[0]);
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
