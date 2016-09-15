package eu.modernmt.facade;

import eu.modernmt.datastream.DataStreamException;
import eu.modernmt.datastream.DataStreamManager;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.BilingualCorpus;

import java.io.IOException;
import java.util.List;

/**
 * Created by davide on 06/09/16.
 */
public class DomainFacade {

    public List<Domain> list() {
        //TODO: stub implementation
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Domain get(int domainId) {
        //TODO: stub implementation
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Domain add(int domainId, BilingualCorpus corpus) throws IOException, DataStreamException {
        DataStreamManager manager = ModernMT.node.getDataStreamManager();

        //TODO: check if domainId exists
        manager.upload(domainId, corpus);

        return new Domain(domainId, null);
    }

    public Domain add(int domainId, String source, String target) throws DataStreamException {
        DataStreamManager manager = ModernMT.node.getDataStreamManager();

        //TODO: check if domainId exists
        manager.upload(domainId, source, target);

        return new Domain(domainId, null);
    }

    public Domain create(String name, BilingualCorpus corpus) {
        //TODO: stub implementation
        throw new UnsupportedOperationException("not yet implemented");
    }

    public Domain create(String name, String source, String target) {
        //TODO: stub implementation
        throw new UnsupportedOperationException("not yet implemented");
    }

}
