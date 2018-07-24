package eu.modernmt.api.actions;

import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.cluster.ServerInfo;
import eu.modernmt.facade.ModernMT;

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
