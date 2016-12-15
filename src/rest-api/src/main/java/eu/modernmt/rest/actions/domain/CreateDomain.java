package eu.modernmt.rest.actions.domain;

import eu.modernmt.cluster.datastream.DataStreamException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "domains", method = HttpMethod.POST)
public class CreateDomain extends ObjectAction<Domain> {

    @Override
    protected Domain execute(RESTRequest req, Parameters _params) throws IOException, DataStreamException, PersistenceException {
        Params params = (Params) _params;
        return ModernMT.domain.create(params.name, params.corpus);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        private final BilingualCorpus corpus;
        private final String name;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);

            Locale sourceLanguage = ModernMT.engine.getSourceLanguage();
            Locale targetLanguage = ModernMT.engine.getTargetLanguage();

            String sourcePath = getString("source_local_file", false, null);
            String targetPath = getString("target_local_file", false, null);
            String tmxPath = getString("tmx_local_file", false, null);

            if (sourcePath == null && targetPath == null) {
                if (tmxPath == null) {
                    throw new ParameterParsingException("tmx_local_file");
                } else {
                    File tmx = new File(tmxPath);
                    if (!tmx.isFile())
                        throw new ParameterParsingException("tmx_local_file", tmx.toString());

                    corpus = new TMXCorpus(tmx, sourceLanguage, targetLanguage);
                }
            } else if (sourcePath == null) {
                throw new ParameterParsingException("source_local_file");
            } else if (targetPath == null) {
                throw new ParameterParsingException("target_local_file");
            } else {
                File source = new File(sourcePath);
                File target = new File(targetPath);

                if (!source.isFile())
                    throw new ParameterParsingException("source_local_file", source.toString());
                if (!target.isFile())
                    throw new ParameterParsingException("target_local_file", target.toString());

                corpus = new ParallelFileCorpus(sourceLanguage, source, targetLanguage, target);
            }

            name = getString("name", false, corpus.getName());
        }
    }

}
