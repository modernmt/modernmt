package eu.modernmt.rest.actions.translation;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.io.FileProxy;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ContextVector;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.actions.util.ContextUtils;
import eu.modernmt.rest.framework.*;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.ContextVectorResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "context-vector", method = HttpMethod.GET)
public class GetContextVector extends ObjectAction<ContextVectorResult> {

    private static File copy(FileProxy source) throws IOException {
        File destination = File.createTempFile("mmt-context", "txt");

        InputStream input = null;
        OutputStream output = null;

        try {
            input = source.getInputStream();
            output = new FileOutputStream(destination, false);

            IOUtils.copyLarge(input, output);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }

        return destination;
    }

    @Override
    protected ContextVectorResult execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, PersistenceException, IOException {
        Params params = (Params) _params;
        Map<Locale, ContextVector> contexts;

        File temp = null;

        try {

            if (params.text != null) {
                contexts = ModernMT.translation.getContextVectors(params.text, params.limit, params.source, params.targets);
            } else {
                boolean gzipped = params.compression != null;
                File file;

                if (params.localFile != null) {
                    if (gzipped)
                        temp = file = copy(FileProxy.wrap(params.localFile, true));
                    else
                        file = params.localFile;
                } else {
                    temp = file = copy(new ParameterFileProxy(params.content, gzipped));
                }

                contexts = ModernMT.translation.getContextVectors(file, params.limit, params.source, params.targets);
            }
        } finally {
            if (temp != null)
                FileUtils.deleteQuietly(temp);
        }

        ContextUtils.resolve(contexts.values());
        return new ContextVectorResult(params.source, contexts, params.backwardCompatible);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public enum FileCompression {
        GZIP
    }

    public static class Params extends Parameters {

        public static final int DEFAULT_LIMIT = 10;

        public final Locale source;
        public final Locale[] targets;
        public final int limit;
        public final String text;
        public final File localFile;
        public final FileParameter content;
        public final FileCompression compression;
        public final boolean backwardCompatible;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            this.limit = getInt("limit", DEFAULT_LIMIT);

            Set<LanguagePair> supportedLanguages = ModernMT.getNode().getEngine().getLanguages().getLanguages();

            Locale sourceLanguage = getLocale("source", null);
            Locale[] targetLanguages = getLocaleArray("targets", null);

            if (sourceLanguage == null && targetLanguages == null) {
                if (supportedLanguages.size() == 1) {
                    LanguagePair engineDirection = supportedLanguages.iterator().next();
                    this.source = engineDirection.source;
                    this.targets = new Locale[1];
                    this.targets[0] = engineDirection.target;
                    this.backwardCompatible = true;
                } else {
                    throw new ParameterParsingException("source");
                }
            } else if (sourceLanguage == null) {
                throw new ParameterParsingException("source");
            } else if (targetLanguages == null) {
                throw new ParameterParsingException("targets");
            } else {
                this.source = sourceLanguage;
                this.targets = targetLanguages;
                this.backwardCompatible = false;
            }

            FileParameter content;
            String localFile;

            if ((content = req.getFile("content")) != null) {
                this.text = null;
                this.localFile = null;
                this.content = content;
                this.compression = getEnum("compression", FileCompression.class, null);
            } else if ((localFile = getString("local_file", false, null)) != null) {
                this.text = null;
                this.localFile = new File(localFile);
                this.content = null;
                this.compression = getEnum("compression", FileCompression.class, null);
            } else {
                this.text = getString("text", false);
                this.localFile = null;
                this.content = null;
                this.compression = null;
            }
        }
    }
}
