package eu.modernmt.rest.actions;

import eu.modernmt.facade.ModernMT;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.ServerStatistics;

import java.io.IOException;

/**
 * Created by davide on 23/12/15.
 */
@Route(aliases = "_stat", method = HttpMethod.GET)
public class ServerStats extends ObjectAction<ServerStatistics> {

    @Override
    protected ServerStatistics execute(RESTRequest req, Parameters params) throws IOException {
        return new ServerStatistics(new ServerStatistics.ClusterStats(ModernMT.cluster.getNodes()));
    }

}
