package eu.modernmt.rest.actions.memory;

import eu.modernmt.data.DataManagerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.io.FileProxy;
import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.ImportJob;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.model.corpus.impl.parallel.CompactFileCorpus;
import eu.modernmt.model.corpus.impl.parallel.ParallelFileCorpus;
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
@Route(aliases = "memories/:id/corpus", method = HttpMethod.POST)
public class AddToMemoryCorpus extends ObjectAction<ImportJob> {

    @Override
    protected ImportJob execute(RESTRequest req, Parameters _params) throws DataManagerException, PersistenceException {
        Params params = (Params) _params;

        if (params.corpus == null)
            return ModernMT.memory.add(params.direction, params.memory, params.source, params.target);
        else
            return ModernMT.memory.add(params.memory, params.corpus);
    }

    @Override
    protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException, TemplateException {
        return new Params(req);
    }

    public enum FileCompression {
        GZIP
    }

    public enum FileType {
        TMX, COMPACT, PARALLEL
    }

    public static class Params extends Parameters {

        private final LanguagePair direction;
        private final long memory;
        private final String source;
        private final String target;
        private final MultilingualCorpus corpus;

        public Params(RESTRequest req) throws ParameterParsingException, TemplateException {
            super(req);

            memory = req.getPathParameterAsLong("id");

            source = getString("sentence", false, null);
            target = getString("translation", false, null);

            if (source == null && target == null) {
                FileType fileType = getEnum("content_type", FileType.class);
                FileCompression fileCompression = getEnum("compression", FileCompression.class, null);

                boolean gzipped = FileCompression.GZIP.equals(fileCompression);

                switch (fileType) {
                    case COMPACT:
                        corpus = new CompactFileCorpus(getFileProxy(null, gzipped));
                        break;
                    case TMX:
                        corpus = new TMXCorpus(getFileProxy(null, gzipped));
                        break;
                    case PARALLEL:
                        Locale sourceLanguage = getLocale("source");
                        Locale targetLanguage = getLocale("target");
                        LanguagePair language = new LanguagePair(sourceLanguage, targetLanguage);

                        corpus = new ParallelFileCorpus(language, getFileProxy("source", gzipped), getFileProxy("target", gzipped));
                        break;
                    default:
                        throw new ParameterParsingException("content_type");
                }

                direction = null;
            } else {
                if (source == null)
                    throw new ParameterParsingException("sentence");
                if (target == null)
                    throw new ParameterParsingException("translation");

                Locale sourceLanguage = getLocale("source");
                Locale targetLanguage = getLocale("target");
                direction = new LanguagePair(sourceLanguage, targetLanguage);

                corpus = null;
            }
        }

        private FileProxy getFileProxy(String prefix, boolean gzipped) throws ParameterParsingException {
            prefix = prefix == null ? "" : (prefix + '_');

            String contentParameter = prefix + "content";
            String fileParameter = prefix + "local_file";

            FileParameter content;

            /*if the corpus is passed by remote*/
            if ((content = req.getFile(contentParameter)) != null) {
                return new ParameterFileProxy(content, gzipped);
                /*else, if the corpus is notified as a local file a*/
            } else {
                File localFile = new File(getString(fileParameter, false));
                if (!localFile.isFile())
                    throw new ParameterParsingException(fileParameter, localFile.toString());

                return new LocalFileProxy(localFile, gzipped);
            }
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

        @Override
        public String toString() {
            return getFilename();
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

        @Override
        public String toString() {
            return getFilename();
        }
    }

}