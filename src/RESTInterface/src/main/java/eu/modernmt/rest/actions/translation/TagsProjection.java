package eu.modernmt.rest.actions.translation;

import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.engine.MasterNode;
import eu.modernmt.engine.TranslationException;
import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
@Route(aliases = "tags-projection", method = HttpMethod.GET)
public class TagsProjection extends ObjectAction<Object> {

    private static class ProjectedTranslation{

        String translation;

        public ProjectedTranslation(String translation) {
            this.translation = translation;
        }
    }

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected Object execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, TranslationException {
        Params params = (Params) _params;
        MasterNode masterNode = server.getMasterNode();
        String processedTranslation;
        if(params.symmetrizationStrategy != null) {
            processedTranslation = masterNode.alignTags(params.sentence, params.translation, params.forceTranslation,
                    params.symmetrizationStrategy);
        }else {
            processedTranslation = masterNode.alignTags(params.sentence, params.translation, params.forceTranslation);
        }
        return new ProjectedTranslation(processedTranslation);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final String sentence;
        public final String translation;
        public final boolean forceTranslation;
        public final Symmetrisation.Type symmetrizationStrategy;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);
            sentence = getString("s", false);
            translation = getString("t", false);
            forceTranslation = getBoolean("f", false);
            int symmetrizationStrategy = getInt("symmetrization", -1);
            if(symmetrizationStrategy >= 0) {
                try {
                    this.symmetrizationStrategy = Symmetrisation.Type.values()[symmetrizationStrategy];
                }catch(Exception e){
                    throw new ParameterParsingException("symmetrization", Integer.toString(symmetrizationStrategy));
                }
            }else{
                this.symmetrizationStrategy = null;
            }
        }
    }
}
