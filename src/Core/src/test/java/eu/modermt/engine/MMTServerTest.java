package eu.modermt.engine;

import eu.modernmt.decoder.Sentence;
import eu.modernmt.engine.MMTServer;
import eu.modernmt.engine.TranslationEngine;

import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 10/12/15.
 */
public class MMTServerTest {

    // -Djava.library.path=/Users/davide/workspaces/mmt/ModernMT/src/Decoder/target:/usr/local/lib/

    static {
        System.setProperty("mmt.engines.path", "/Users/davide/workspaces/mmt/ModernMT/engines");
    }

    public static void main(String[] args) throws Throwable {
        TranslationEngine engine = TranslationEngine.get("default");
        MMTServer server = new MMTServer(engine);

        Thread.sleep(3000);
//
//        System.out.println(server.translate(new Sentence("ciao mondo")));

        Thread.sleep(3000);

        server.shutdown();
        server.awaitTermination(1, TimeUnit.DAYS);
    }

}
