package eu.modernmt.api.actions.memory;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Memory;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.CollectionAction;
import eu.modernmt.api.framework.routing.Route;

import java.util.Collection;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = {"memories", "domains"}, method = HttpMethod.GET)
public class GetAllMemories extends CollectionAction<Memory> {

    @Override
    protected Collection<Memory> execute(RESTRequest req, Parameters _params) throws PersistenceException {
        return ModernMT.memory.list();
    }

}
