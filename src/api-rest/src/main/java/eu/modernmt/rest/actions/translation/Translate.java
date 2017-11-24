package eu.modernmt.rest.actions.translation;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.facade.TranslationFacade;
import eu.modernmt.facade.exceptions.TranslationException;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.actions.util.ContextUtils;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.TranslationResponse;

/**
 * Created by davide on 17/12/15.
 */
@Route(aliases = "translate", method = HttpMethod.GET)
public class Translate extends ObjectAction<TranslationResponse> {

    public static final int MAX_QUERY_LENGTH = 5000;

    @Override
    protected TranslationResponse execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, PersistenceException, TranslationException {
        Params params = (Params) _params;

        TranslationResponse result = new TranslationResponse();
        result.verbose = params.verbose;

        if (params.context != null) {
            result.translation = ModernMT.translation.get(params.direction, params.query, params.context, params.nbest, params.priority);
        } else if (params.contextString != null) {
            result.context = ModernMT.translation.getContextVector(params.direction, params.contextString, params.contextLimit);
            result.translation = ModernMT.translation.get(params.direction, params.query, result.context, params.nbest, params.priority);
        } else {
            result.translation = ModernMT.translation.get(params.direction, params.query, params.nbest, params.priority);
        }


        if (result.context != null)
            ContextUtils.resolve(result.context);

        return result;
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final LanguagePair direction;
        public final String query;
        public final ContextVector context;
        public final String contextString;
        public final int contextLimit;
        public final int nbest;
        public final TranslationFacade.Priority priority;
        public final boolean verbose;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            query = getString("q", true);
            if (query.length() > MAX_QUERY_LENGTH)
                throw new ParameterParsingException("q", query.substring(0, 10) + "...",
                        "max query length of " + MAX_QUERY_LENGTH + " exceeded");

            LanguagePair engineDirection = ModernMT.getNode().getEngine().getLanguages().asSingleLanguagePair();
            direction = engineDirection != null ?
                    getLanguagePair("source", "target", engineDirection) :
                    getLanguagePair("source", "target");

            contextLimit = getInt("context_limit", 10);
            nbest = getInt("nbest", 0);

            priority = getEnum("priority", TranslationFacade.Priority.class, TranslationFacade.Priority.NORMAL);

            verbose = getBoolean("verbose", false);

            String weights = getString("context_vector", false, null);

            if (weights != null) {
                context = ContextUtils.parseParameter("context_vector", weights);
                contextString = null;
            } else {
                context = null;
                contextString = getString("context", false, null);
            }
        }
    }
}