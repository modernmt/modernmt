package eu.modernmt.rest.actions.memory;

import eu.modernmt.data.DataManagerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ImportJob;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = {"memories/:id/corpus", "domains/:id/corpus"}, method = HttpMethod.PUT)
public class UpdateMemoryContribution extends ObjectAction<ImportJob> {

    @Override
    protected ImportJob execute(RESTRequest req, Parameters _params) throws DataManagerException, PersistenceException {
        Params params = (Params) _params;
        return ModernMT.memory.replace(params.direction, params.memory, params.sentence, params.translation,
                params.previousSentence, params.previousTranslation);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final LanguagePair direction;
        private final long memory;
        private final String sentence;
        private final String translation;
        private final String previousSentence;
        private final String previousTranslation;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);

            memory = req.getPathParameterAsLong("id");

            sentence = getString("sentence", false);
            translation = getString("translation", false);
            previousSentence = getString("old_sentence", false);
            previousTranslation = getString("old_translation", false);

            Language sourceLanguage = getLanguage("source");
            Language targetLanguage = getLanguage("target");
            direction = new LanguagePair(sourceLanguage, targetLanguage);
        }
    }

}