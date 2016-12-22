package eu.modernmt.rest.actions.domain;

import eu.modernmt.cluster.datastream.DataStreamException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.io.FileProxy;
import eu.modernmt.model.Domain;
import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.InlineParallelFileCorpus;
import eu.modernmt.model.corpus.impl.tmx.TMXCorpus;
import eu.modernmt.persistence.PersistenceException;
import eu.modernmt.rest.framework.FileParameter;
import eu.modernmt.rest.framework.HttpMethod;
import eu.modernmt.rest.framework.Parameters;
import eu.modernmt.rest.framework.RESTRequest;
import eu.modernmt.rest.framework.actions.ObjectAction;
import eu.modernmt.rest.framework.routing.Route;
import eu.modernmt.rest.framework.routing.TemplateException;

import java.io.*;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Created by davide on 15/12/15.
 */
@Route(aliases = "domains", method = HttpMethod.POST)
public class CreateDomain extends ObjectAction<Domain> {

    public enum FileCompression {
        GZIP
    }

    public enum FileType {
        TMX, INLINE
    }

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

            FileType fileType = getEnum("content_type", FileType.class);
            FileCompression fileCompression = getEnum("content_compression", FileCompression.class, null);

            boolean gzipped = FileCompression.GZIP.equals(fileCompression);
            FileProxy fileProxy;

            FileParameter content;
            if ((content = req.getFile("content")) != null) {
                fileProxy = new ParameterFileProxy(content, gzipped);
            } else {
                File localFile = new File(getString("local_file", false));
                if (!localFile.isFile())
                    throw new ParameterParsingException("local_file", localFile.toString());

                fileProxy = new LocalFileProxy(localFile, gzipped);
            }

            Locale sourceLanguage = ModernMT.engine.getSourceLanguage();
            Locale targetLanguage = ModernMT.engine.getTargetLanguage();

            switch (fileType) {
                case INLINE:
                    corpus = new InlineParallelFileCorpus(sourceLanguage, targetLanguage, fileProxy);
                    break;
                case TMX:
                    corpus = new TMXCorpus(fileProxy, sourceLanguage, targetLanguage);
                    break;
                default:
                    throw new ParameterParsingException("content_type");
            }

            name = getString("name", false, corpus.getName());
        }
    }

    private static class LocalFileProxy implements FileProxy {

        private final File file;
        private final boolean gzipped;

        public LocalFileProxy(File file, boolean gzipped) {
            this.file = file;
            this.gzipped = gzipped;
        }

        @Override
        public String getFilename() {
            return file.getName();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream stream = new FileInputStream(file);
            if (gzipped)
                stream = new GZIPInputStream(stream);

            return stream;
        }

        @Override
        public OutputStream getOutputStream(boolean append) throws IOException {
            throw new UnsupportedOperationException();
        }

    }

    private static class ParameterFileProxy implements FileProxy {

        private final FileParameter file;
        private final boolean gzipped;

        public ParameterFileProxy(FileParameter file, boolean gzipped) {
            this.file = file;
            this.gzipped = gzipped;
        }

        @Override
        public String getFilename() {
            return file.getFilename();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream stream = file.getInputStream();
            if (gzipped)
                stream = new GZIPInputStream(stream);

            return stream;
        }

        @Override
        public OutputStream getOutputStream(boolean append) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

}
