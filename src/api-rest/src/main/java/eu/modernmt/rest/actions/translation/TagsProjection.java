package eu.modernmt.rest.actions.translation;

import eu.modernmt.aligner.Aligner;
import eu.modernmt.aligner.AlignerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.ProjectedTranslation;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
@Route(aliases = "tags-projection", method = HttpMethod.GET)
public class TagsProjection extends ObjectAction<ProjectedTranslation> {

    @Override
    protected ProjectedTranslation execute(RESTRequest req, Parameters _params) throws AlignerException, ProcessingException {
        Params params = (Params) _params;

        Translation taggedTranslation;
        if (params.symmetrizationStrategy != null)
            taggedTranslation = ModernMT.tags.project(params.direction, params.sentence, params.translation, params.symmetrizationStrategy);
        else
            taggedTranslation = ModernMT.tags.project(params.direction, params.sentence, params.translation);

        ProjectedTranslation result = new ProjectedTranslation(taggedTranslation.toString());

        if (params.showDetails) {
            result.setSourceTokens(stringifyTokens(taggedTranslation.getSource().getWords()));
            result.setTargetTokens(stringifyTokens(taggedTranslation.getWords()));
            result.setAlignment(taggedTranslation.getWordAlignment());
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

        public final LanguagePair direction;
        public final String sentence;
        public final String translation;
        public final Aligner.SymmetrizationStrategy symmetrizationStrategy;
        public final boolean showDetails;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            LanguagePair engineDirection = ModernMT.getNode().getEngine().getLanguages().asSingleLanguagePair();
            this.direction = engineDirection != null ?
                    getLanguagePair("source", "target", engineDirection) :
                    getLanguagePair("source", "target");
            this.sentence = getString("text", false);
            this.translation = getString("translation", false);
            this.showDetails = getBoolean("verbose", false);
            this.symmetrizationStrategy = getEnum("symmetrization", Aligner.SymmetrizationStrategy.class, null);
        }
    }
}
