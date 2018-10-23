package eu.modernmt.api.actions;

import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.cluster.ServerInfo;
import eu.modernmt.facade.ModernMT;

/**
 * Created by davide on 23/12/15.
 */
@Route(aliases = "", method = HttpMethod.GET, log = false)
public class GetServerInfo extends ObjectAction<ServerInfo> {

    @Override
    protected ServerInfo execute(RESTRequest req, Parameters _params) {
        Params params = (Params) _params;
        return ModernMT.info(params.localhost);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final boolean localhost;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            localhost = getBoolean("localhost", false);
        }
    }

}
