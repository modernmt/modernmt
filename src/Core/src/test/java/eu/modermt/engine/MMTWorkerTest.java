//package eu.modermt.engine;
//
//import eu.modernmt.engine.MMTWorker;
//import eu.modernmt.engine.TranslationEngine;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * Created by davide on 10/12/15.
// */
//public class MMTWorkerTest {
//
//    static {
//        System.setProperty("mmt.engines.path", "/Users/davide/workspaces/mmt/ModernMT/engines");
//    }
//
//    public static void main(String[] args) throws Throwable {
//        TranslationEngine engine = TranslationEngine.get("default");
//        MMTWorker worker = new MMTWorker(engine);
//
//        worker.awaitTermination(1, TimeUnit.DAYS);
//    }
//}
