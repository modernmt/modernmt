package eu.modernmt.rest.actions.translation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.decoder.TranslationSession;
import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "sessions", method = HttpMethod.POST)
public class CreateTranslationSession extends ObjectAction<TranslationSession> {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected TranslationSession execute(RESTRequest req, Parameters _params) throws IOException {
        Params params = (Params) _params;
        return server.getMMTServer().createTranslationSession(params.context);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final List<ContextDocument> context;

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

    public static List<ContextDocument> parseContext(JsonElement root) throws JsonParseException {
        JsonArray array = root.getAsJsonArray();
        ArrayList<ContextDocument> list = new ArrayList<>(array.size());

        for (JsonElement e : array) {
            JsonObject json = e.getAsJsonObject();
            String id = json.get("id").getAsString();
            float score = json.get("score").getAsFloat();

            list.add(new ContextDocument(id, score));
        }

        return list;
    }
}
