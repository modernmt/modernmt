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
import java.util.regex.Pattern;

/**
 * Created by davide on 20/10/16.
 */
public class ContextUtils {

    private static final Pattern PARAMETER_REGEX = Pattern.compile("([1-9][0-9]*:0\\.[0-9]*)(,[1-9][0-9]*:0\\.[0-9]*)*");

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
        if (!PARAMETER_REGEX.matcher(value).matches())
            throw new Parameters.ParameterParsingException(name, value);

        String[] elements = value.split(",");
        List<ContextScore> context = new ArrayList<>(elements.length);

        for (String element : elements) {
            String[] keyvalue = element.split(":");
            Domain domain = new Domain(Integer.parseInt(keyvalue[0]));
            float score = Float.parseFloat(keyvalue[1]);

            context.add(new ContextScore(domain, score));
        }

        return context;
    }
}
