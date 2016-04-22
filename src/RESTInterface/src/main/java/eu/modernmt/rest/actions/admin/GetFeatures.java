package eu.modernmt.rest.actions.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import eu.modernmt.core.facade.ModernMT;
import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.JSONObjectAction;
import eu.modernmt.rest.framework.routing.Route;

import java.util.Map;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "decoder/features", method = HttpMethod.GET)
public class GetFeatures extends JSONObjectAction {

    @Override
    protected JsonObject execute(RESTRequest req, Parameters params) {
        JsonObject result = new JsonObject();

        Map<MosesFeature, float[]> features = ModernMT.decoder.getFeatureWeights();
        for (Map.Entry<MosesFeature, float[]> entry : features.entrySet()) {
            MosesFeature feature = entry.getKey();
            JsonArray weights = null;

            if (feature.isTunable()) {
                weights = new JsonArray();

                for (float w : entry.getValue())
                    weights.add(w == MosesFeature.UNTUNEABLE_COMPONENT ? null : w);
            }

            result.add(feature.getName(), weights);
        }

        return result;
    }

}
