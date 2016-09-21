package eu.modernmt.rest.actions.domain;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Domain;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.CollectionAction;
import eu.modernmt.rest.framework.routing.Route;

import java.util.Collection;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "domains", method = HttpMethod.GET)
public class GetAllDomains extends CollectionAction<Domain> {

    @Override
    protected Collection<Domain> execute(RESTRequest req, Parameters _params) throws PersistenceException {
        return ModernMT.domain.list();
    }

}
