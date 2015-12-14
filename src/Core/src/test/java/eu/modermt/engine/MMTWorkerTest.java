package eu.modermt.engine;

import eu.modernmt.engine.MMTWorker;
import eu.modernmt.engine.TranslationEngine;

/**
 * Created by davide on 10/12/15.
 */
public class MMTWorkerTest {

    static {
        System.setProperty("mmt.engines.path", "/Users/davide/workspaces/mmt/ModernMT/engines");
        System.setProperty("mmt.tokenizer.models.path", "/Users/davide/workspaces/mmt/ModernMT/data/tokenizer/models");
    }

    public static void main(String[] args) throws Throwable {
        TranslationEngine engine = TranslationEngine.get("worker-default");
        MMTWorker worker = new MMTWorker(engine, 1);
        worker.start();
    }
}
