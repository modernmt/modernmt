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

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "domains/:id", method = HttpMethod.PUT)
public class AddToDomain extends ObjectAction<Domain> {

    @Override
    protected Domain execute(RESTRequest req, Parameters _params) throws IOException, DataStreamException, PersistenceException {
        Params params = (Params) _params;

        BilingualCorpus corpus = new ParallelFileCorpus(null, params.source, null, params.target);

        return ModernMT.domain.add(params.id, corpus);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final int id;
        private final File source;
        private final File target;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);

            id = req.getPathParameterAsInt("id");

            source = new File(req.getParameter("source_local_file"));
            target = new File(req.getParameter("target_local_file"));

            if (!source.isFile())
                throw new ParameterParsingException("source_local_file", source.toString());
            if (!target.isFile())
                throw new ParameterParsingException("target_local_file", target.toString());
        }
    }

}
