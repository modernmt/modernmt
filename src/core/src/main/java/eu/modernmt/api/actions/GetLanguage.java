package eu.modernmt.api.actions;

import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.framework.routing.TemplateException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;

@Route(aliases = "languages/:source/:target", method = HttpMethod.GET)
public class GetLanguage extends ObjectAction<LanguageDirection> {

    @Override
    protected LanguageDirection execute(RESTRequest req, Parameters _params) {
        Params params = (Params) _params;
        return ModernMT.translation.mapLanguage(params.language);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final LanguageDirection language;

        public Params(RESTRequest req) throws TemplateException {
            super(req);

            Language source, target;

            try {
                source = Language.fromString(req.getPathParameter("source"));
            } catch (IllegalArgumentException e) {
                throw new TemplateException("source");
            }

            try {
                target = Language.fromString(req.getPathParameter("target"));
            } catch (IllegalArgumentException e) {
                throw new TemplateException("target");
            }

            this.language = new LanguageDirection(source, target);
        }
    }

}
