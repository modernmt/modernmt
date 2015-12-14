package eu.modermt.engine;

import eu.modernmt.decoder.moses.MosesFeature;
import eu.modernmt.engine.MMTServer;
import eu.modernmt.engine.TranslationEngine;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 10/12/15.
 */
public class MMTServerTest {

    static {
        System.setProperty("mmt.engines.path", "/Users/davide/workspaces/mmt/ModernMT/engines");
    }

    public static void main(String[] args) throws Throwable {
        TranslationEngine engine = TranslationEngine.get("worker-default");
        MMTServer server = new MMTServer(engine);
        server.start();

        Thread.sleep(3000);

//        Map<MosesFeature, float[]> weights = server.getFeatureWeights();
//        for (MosesFeature feature : weights.keySet()) {
//            System.out.print(feature.getName());
//            if (feature.isTunable())
//                System.out.println(" " + Arrays.toString(weights.get(feature)));
//            else
//                System.out.println(" UNTUNEABLE");
//        }
//
//        HashMap<String, float[]> setweights = new HashMap<>();
//        setweights.put("Distortion0", new float[]{111.f});
//        setweights.put("DM0", new float[]{2.f, 3.f, 4.f, 5.f, 6.f, 7.f, 8.f, 9.f});
//        setweights.put("LM0", new float[]{10.f});
//        setweights.put("WordPenalty0", new float[]{11.f});
//        setweights.put("PhrasePenalty0", new float[]{12.f});
//        setweights.put("PT0", new float[]{13.f, 14.f, 15.f, 16.f, 17.f});

//        server.setFeatureWeights(setweights);

        System.out.println(server.translate("d'altra parte non credo sia giusto!", true));

        Thread.sleep(3000);

        server.shutdown();
        server.awaitTermination(1, TimeUnit.DAYS);
    }

}
