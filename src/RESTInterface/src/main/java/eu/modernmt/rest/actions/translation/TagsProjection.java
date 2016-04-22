package eu.modernmt.rest.actions.translation;

import eu.modernmt.aligner.symal.Symmetrisation;
import eu.modernmt.core.facade.ModernMT;
import eu.modernmt.core.facade.error.TranslationException;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
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

    @Override
    protected Object execute(RESTRequest req, Parameters _params) throws TranslationException {
        Params params = (Params) _params;

        Translation taggedTranslation;
        if (params.symmetrizationStrategy != null) {
            taggedTranslation = ModernMT.tags.project(params.sentence, params.translation, params.symmetrizationStrategy, params.inverted);
        } else {
            taggedTranslation = ModernMT.tags.project(params.sentence, params.translation, params.inverted);
        }

        ProjectedTranslation result;
        if (params.showDetails) {
            String[] sourceToken = stringifyTokens(taggedTranslation.getSource().getWords());
            String[] targetToken = stringifyTokens(taggedTranslation.getWords());
            int[][] alignments = taggedTranslation.getAlignment();
            result = new ExhaustiveProjectedTranslation(taggedTranslation.toString(), sourceToken, targetToken,
                    alignments);
        } else {
            result = new ProjectedTranslation(taggedTranslation.toString());
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
        public final Symmetrisation.Strategy symmetrizationStrategy;
        public final boolean showDetails;
        public final boolean inverted;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);
            this.sentence = getString("s", false);
            this.translation = getString("t", false);
            this.showDetails = getBoolean("d", false);
            int symmetrizationStrategy = getInt("symmetrization", -1);
            if (symmetrizationStrategy >= 0) {
                try {
                    this.symmetrizationStrategy = Symmetrisation.Strategy.values()[symmetrizationStrategy];
                } catch (Exception e) {
                    throw new ParameterParsingException("symmetrization", Integer.toString(symmetrizationStrategy));
                }
            } else {
                this.symmetrizationStrategy = null;
            }
            this.inverted = getBoolean("i", false);
        }
    }
}
