package eu.modernmt.api.actions;

import eu.modernmt.api.framework.HttpMethod;
import eu.modernmt.api.framework.Parameters;
import eu.modernmt.api.framework.RESTRequest;
import eu.modernmt.api.framework.actions.CollectionAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.lang.LanguageDirection;

import java.util.Collection;

@Route(aliases = "languages", method = HttpMethod.GET)
public class GetLanguages extends CollectionAction<LanguageDirection> {

    @Override
    protected Collection<LanguageDirection> execute(RESTRequest req, Parameters _params) {
        return ModernMT.translation.getLanguages();
    }

}
