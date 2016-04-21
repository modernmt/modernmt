package eu.modernmt.core.facade;

import eu.modernmt.core.cluster.Client;

/**
 * Created by davide on 20/04/16.
 */
public class ModernMT {

    protected static Client client;

    public static void setClient(Client _client) {
        client = _client;
    }

    public static final DecoderFacade decoder = new DecoderFacade();
    public static final ContextAnalyzerFacade context = new ContextAnalyzerFacade();
    public static final TagFacade tags = new TagFacade();

}
