package eu.modernmt.rest.actions.translation;

import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextDocument;
import eu.modernmt.rest.RESTServer;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.CollectionAction;
import eu.modernmt.rest.framework.routing.Route;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.io.RuntimeIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "context", method = HttpMethod.GET)
public class GetContext extends CollectionAction<ContextDocument> {

    private RESTServer server = RESTServer.getInstance();

    @Override
    protected Collection<ContextDocument> execute(RESTRequest req, Parameters _params) throws ContextAnalyzerException {
        Params params = (Params) _params;

        if (params.file == null) {
            return server.getMasterNode().getContext(params.context, params.limit);
        } else {
            try {
                return server.getMasterNode().getContext(params.file, params.limit);
            } finally {
                if (params.deleteOnExit)
                    FileUtils.deleteQuietly(params.file);
            }
        }
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
        return new Params(req);
    }

    public static class Params extends Parameters {

        // TODO: File from body should be made more secure (check real stream size and ignore too large files)

        private final Logger logger = LoggerFactory.getLogger(getClass());

        public static final int DEFAULT_LIMIT = 10;
        public static final int MAX_CONTEXT_IN_RAM = 2 * 1024;

        public final int limit;
        public final File file;
        public final String context;
        public final boolean deleteOnExit;

        public Params(RESTRequest req) throws ParameterParsingException {
            super(req);

            limit = getInt("limit", DEFAULT_LIMIT);

            Reader body;
            String filename;

            try {
                body = req.getPlainTextContent();
            } catch (IOException e) {
                throw new ParameterParsingException(e);
            }

            if (body != null) {
                int length = req.getContentLength();

                if (length < 0 || length > MAX_CONTEXT_IN_RAM) {
                    FileWriter output = null;
                    File tempFile = null;
                    boolean success = false;

                    try {
                        tempFile = File.createTempFile("mmt_context", ".txt");
                        tempFile.deleteOnExit();

                        output = new FileWriter(tempFile, false);
                        long size = IOUtils.copyLarge(req.getPlainTextContent(), output);

                        if (logger.isDebugEnabled())
                            logger.debug("Read " + size + " bytes for context analyzer document input (declared " + length + ")");

                        success = true;
                    } catch (IOException e) {
                        throw new RuntimeIOException(e);
                    } finally {
                        IOUtils.closeQuietly(output);
                        if (!success)
                            FileUtils.deleteQuietly(tempFile);
                    }

                    file = tempFile;
                    context = null;
                    deleteOnExit = true;
                } else {
                    try {
                        StringWriter writer = new StringWriter();
                        long size = IOUtils.copyLarge(req.getPlainTextContent(), writer, 0L, length);

                        if (logger.isDebugEnabled())
                            logger.debug("Read " + size + " bytes for context analyzer document input (declared " + length + ")");

                        file = null;
                        context = writer.toString();
                        deleteOnExit = false;
                    } catch (IOException e) {
                        throw new RuntimeIOException(e);
                    }
                }
            } else if ((filename = getString("local_file", false, null)) != null) {
                file = new File(filename);
                context = null;
                deleteOnExit = false;
            } else {
                file = null;
                context = getString("text", false);
                deleteOnExit = false;
            }
        }
    }
}
