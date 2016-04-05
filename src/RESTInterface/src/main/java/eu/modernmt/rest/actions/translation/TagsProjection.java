package eu.modernmt.rest.actions.translation;

import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.engine.MasterNode;
import eu.modernmt.engine.TranslationException;
import eu.modernmt.model.AutomaticTaggedTranslation;
import eu.modernmt.model.Token;
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

    private static class ProjectedTranslation {

        final String translation;

        public ProjectedTranslation(String translation) {
            this.translation = translation;
        }
    }

    private static class ExhaustiveProjectedTranslation extends ProjectedTranslation {

        final String[] sourceToken;
        final String[] targetToken;
        final int[][] alignments;

        public ExhaustiveProjectedTranslation(String translation, String[] sourceToken, String[] targetToken,
                                              int[][] alignments) {
            super(translation);
            this.sourceToken = sourceToken;
            this.targetToken = targetToken;
            this.alignments = alignments;
        }
    }

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected Object execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, TranslationException {
        Params params = (Params) _params;
        MasterNode masterNode = server.getMasterNode();
        AutomaticTaggedTranslation taggedTranslation;
        if (params.symmetrizationStrategy != null) {
            taggedTranslation = masterNode.alignTags(params.sentence, params.translation, params.forceTranslation,
                    params.symmetrizationStrategy);
        } else {
            taggedTranslation = masterNode.alignTags(params.sentence, params.translation, params.forceTranslation);
        }
        ProjectedTranslation result;
        if (params.showDetails) {
            String[] sourceToken = stringifyTokens(taggedTranslation.getSource().getWords());
            String[] targetToken = stringifyTokens(taggedTranslation.getWords());
            int[][] alignments = taggedTranslation.getAlignment();
            result = new ExhaustiveProjectedTranslation(taggedTranslation.getAutomaticTaggedTranslation(), sourceToken, targetToken,
                    alignments);
        } else {
            result = new ProjectedTranslation(taggedTranslation.getAutomaticTaggedTranslation());
        }
        return result;
    }

    private static String[] stringifyTokens(Token[] tokens) {
        String[] strings = new String[tokens.length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = tokens[i].getText();
        }
        return strings;
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
        public final boolean showDetails;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);
            this.sentence = getString("s", false);
            this.translation = getString("t", false);
            this.forceTranslation = getBoolean("f", false);
            this.showDetails = getBoolean("d", false);
            int symmetrizationStrategy = getInt("symmetrization", -1);
            if (symmetrizationStrategy >= 0) {
                try {
                    this.symmetrizationStrategy = Symmetrisation.Type.values()[symmetrizationStrategy];
                } catch (Exception e) {
                    throw new ParameterParsingException("symmetrization", Integer.toString(symmetrizationStrategy));
                }
            } else {
                this.symmetrizationStrategy = null;
            }
        }
    }
}
