package eu.modernmt.rest.actions.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.decoder.DecoderFeature;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.VoidAction;
import eu.modernmt.rest.framework.routing.Route;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "decoder/features", method = HttpMethod.PUT)
public class SetFeatures extends VoidAction {

    @Override
    protected void execute(RESTRequest req, Parameters _params) {
        Params params = (Params) _params;
        ModernMT.decoder.setFeatureWeights(params.weights);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        public final Map<String, float[]> weights;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            JsonObject json = req.getJSONObject();
            if (json == null)
                throw new ParameterParsingException();

            try {
                weights = new HashMap<>();

                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    String feature = entry.getKey();
                    JsonElement value = entry.getValue();

                    if (value.isJsonNull())
                        continue;

                    JsonArray array = value.getAsJsonArray();
                    float[] ws = new float[array.size()];

                    for (int i = 0; i < ws.length; i++) {
                        JsonElement e = array.get(i);
                        ws[i] = e.isJsonNull() ? DecoderFeature.UNTUNEABLE_COMPONENT : e.getAsFloat();
                    }

                    weights.put(feature, ws);
                }
            } catch (JsonParseException e) {
                throw new ParameterParsingException(e);
            }
        }

    }
}
