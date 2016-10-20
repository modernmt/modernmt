package eu.modernmt.rest.actions;

import eu.modernmt.context.ContextScore;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.PersistenceException;

import java.util.ArrayList;
import java.util.Collection;
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
}
