package eu.modernmt.rest.actions.domain;

import eu.modernmt.datastream.DataStreamException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "domains", method = HttpMethod.POST)
public class CreateDomain extends ObjectAction<Domain> {

    @Override
    protected Domain execute(RESTRequest req, Parameters _params) throws IOException, DataStreamException, PersistenceException {
        Params params = (Params) _params;

        BilingualCorpus corpus = new ParallelFileCorpus(null, params.source, null, params.target);

        return ModernMT.domain.create(params.name, corpus);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final File source;
        private final File target;
        private final String name;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);

            source = new File(getString("source_local_file", false));
            target = new File(getString("target_local_file", false));

            if (!source.isFile())
                throw new ParameterParsingException("source_local_file", source.toString());
            if (!target.isFile())
                throw new ParameterParsingException("target_local_file", target.toString());

            name = getString("name", false, FilenameUtils.getBaseName(source.getName()));
        }
    }

}
