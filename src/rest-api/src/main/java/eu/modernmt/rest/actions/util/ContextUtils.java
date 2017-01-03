package eu.modernmt.rest.actions.util;

import eu.modernmt.context.ContextScore;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by davide on 20/10/16.
 */
public class ContextUtils {

    public static void resolve(Collection<ContextScore> context) throws PersistenceException {
        ArrayList<Integer> ids = new ArrayList<>(context.size());
        for (ContextScore score : context)
            ids.add(score.getDomain().getId());

        Map<Integer, Domain> domains = ModernMT.domain.get(ids);
        for (ContextScore score : context) {
            int id = score.getDomain().getId();
            score.setDomain(domains.get(id));
        }
    }

    public static List<ContextScore> parseParameter(String name, String value) throws Parameters.ParameterParsingException {
        String[] elements = value.split(",");
        List<ContextScore> context = new ArrayList<>(elements.length);

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

            context.add(new ContextScore(new Domain(domainId), score));
        }

        return context;
    }

}
