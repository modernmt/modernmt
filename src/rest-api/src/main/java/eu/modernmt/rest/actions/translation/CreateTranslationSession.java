package eu.modernmt.rest.actions.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import eu.modernmt.context.ContextScore;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Domain;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "sessions", method = HttpMethod.POST)
public class CreateTranslationSession extends ObjectAction<TranslationSession> {

    @Override
    protected TranslationSession execute(RESTRequest req, Parameters _params) {
        Params params = (Params) _params;
        return ModernMT.decoder.openSession(params.context);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final List<ContextScore> context;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            JsonArray array = req.getJSONArray();
            if (array == null)
                throw new ParameterParsingException();

            try {
                context = parseContext(array);
            } catch (JsonParseException e) {
                throw new ParameterParsingException(e);
            }
        }

    }

    public static List<ContextScore> parseContext(JsonElement root) throws JsonParseException {
        JsonArray array = root.getAsJsonArray();
        ArrayList<ContextScore> list = new ArrayList<>(array.size());

        for (JsonElement e : array) {
            JsonObject json = e.getAsJsonObject();

            JsonElement domainElement = json.get("domain");
            if (domainElement == null)
                throw new JsonParseException("Missing value 'domain'");
            JsonElement scoreElement = json.get("score");
            if (scoreElement == null)
                throw new JsonParseException("Missing value 'score'");

            if (!domainElement.isJsonPrimitive())
                domainElement = domainElement.getAsJsonObject().get("id");

            int id = (int) domainElement.getAsLong(); // unsigned int
            float score = scoreElement.getAsFloat();

            list.add(new ContextScore(new Domain(id, null), score));
        }

        return list;
    }
}
