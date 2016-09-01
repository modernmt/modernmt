package eu.modernmt.rest.actions.translation;

import eu.modernmt.aligner.AlignerException;
import eu.modernmt.aligner.symal.SymmetrizationStrategy;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.facade.exceptions.validation.LanguagePairNotSupportedException;
import eu.modernmt.model.Token;
import eu.modernmt.model.Translation;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.ProjectedTranslation;

import java.util.Locale;

/**
 * Created by lucamastrostefano on 15/03/16.
 */
@Route(aliases = "tags-projection", method = HttpMethod.GET)
public class TagsProjection extends ObjectAction<ProjectedTranslation> {

    @Override
    protected ProjectedTranslation execute(RESTRequest req, Parameters _params) throws AlignerException, LanguagePairNotSupportedException {
        Params params = (Params) _params;

        ModernMT.tags.isLanguagesSupported(params.sourceLanguage, params.targetLanguage);

        Translation taggedTranslation;
        if (params.symmetrizationStrategy != null) {
            taggedTranslation = ModernMT.tags.project(params.sentence, params.translation, params.sourceLanguage, params.targetLanguage, params.symmetrizationStrategy);
        } else {
            taggedTranslation = ModernMT.tags.project(params.sentence, params.translation, params.sourceLanguage, params.targetLanguage);
        }

        ProjectedTranslation result = new ProjectedTranslation(taggedTranslation.toString());

        if (params.showDetails) {
            result.setSourceTokens(stringifyTokens(taggedTranslation.getSource().getWords()));
            result.setTargetTokens(stringifyTokens(taggedTranslation.getWords()));
            result.setAlignment(taggedTranslation.getAlignment());
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
        public final SymmetrizationStrategy symmetrizationStrategy;
        public final boolean showDetails;
        public final Locale sourceLanguage;
        public final Locale targetLanguage;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);
            this.sentence = getString("s", false);
            this.translation = getString("t", false);
            this.showDetails = getBoolean("d", false);

            String symmetrizationStrategy = getString("symmetrization", false, null);
            if (symmetrizationStrategy != null) {
                try {
                    this.symmetrizationStrategy = SymmetrizationStrategy.forName(symmetrizationStrategy);
                } catch (IllegalArgumentException e) {
                    throw new ParameterParsingException("symmetrization", symmetrizationStrategy);
                }
            } else {
                this.symmetrizationStrategy = null;
            }

            this.sourceLanguage = Locale.forLanguageTag(getString("sl", false));
            this.targetLanguage = Locale.forLanguageTag(getString("tl", false));
        }
    }
}
