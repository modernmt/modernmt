package eu.modernmt.rest.actions;

import eu.modernmt.cluster.ServerInfo;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;

import java.io.IOException;

/**
 * Created by davide on 23/12/15.
 */
@Route(aliases = "", method = HttpMethod.GET, log = false)
public class GetServerInfo extends ObjectAction<ServerInfo> {

    @Override
    protected ServerInfo execute(RESTRequest req, Parameters params) throws IOException {
        return ModernMT.info();
    }

}
