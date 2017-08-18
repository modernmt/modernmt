package eu.modernmt.rest.actions.translation;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.model.ContextVector;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.actions.util.ContextUtils;
import eu.modernmt.rest.framework.FileParameter;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.model.ContextVectorResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "context-vector", method = HttpMethod.GET)
public class GetContextVector extends ObjectAction<ContextVectorResult> {

    public enum FileCompression {
        GZIP
    }

    private static void copy(FileParameter source, File destination, FileCompression compression) throws IOException {
        Reader reader = null;
        Writer writer = null;

        try {
            InputStream input = source.getInputStream();

            if (compression != null) {
                switch (compression) {
                    case GZIP:
                        input = new GZIPInputStream(input);
                        break;
                }
            }

            reader = new InputStreamReader(input, Charset.defaultCharset());
            writer = new OutputStreamWriter(new FileOutputStream(destination, false), Charset.defaultCharset());

            IOUtils.copyLarge(reader, writer);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }

    @Override
    protected ContextVectorResult execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException, PersistenceException, IOException {
        Params params = (Params) _params;
        Map<Locale, ContextVector> contexts;

        if (params.text != null) {
            contexts = ModernMT.translation.getContextVectors(params.text, params.limit, params.source, params.targets);
        } else if (params.localFile != null) {
            contexts = ModernMT.translation.getContextVectors(params.localFile, params.limit, params.source, params.targets);
        } else {
            File file = null;

            try {
                file = File.createTempFile("mmt-context", "txt");
                copy(params.content, file, params.compression);
                contexts = ModernMT.translation.getContextVectors(file, params.limit, params.source, params.targets);
            } finally {
                FileUtils.deleteQuietly(file);
            }
        }

        ContextUtils.resolve(contexts.values());
        return new ContextVectorResult(params.source, contexts);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
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

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            this.limit = getInt("limit", DEFAULT_LIMIT);
            this.source = getLocale("source");
            this.targets = getLocaleArray("targets");

            FileParameter content;
            String localFile;

            if ((content = req.getFile("content")) != null) {
                this.text = null;
                this.localFile = null;
                this.content = content;
                this.compression = getEnum("content_compression", FileCompression.class, null);
            } else if ((localFile = getString("local_file", false, null)) != null) {
                this.text = null;
                this.localFile = new File(localFile);
                this.content = null;
                this.compression = null;
            } else {
                this.text = getString("text", false);
                this.localFile = null;
                this.content = null;
                this.compression = null;
            }
        }
    }
}
