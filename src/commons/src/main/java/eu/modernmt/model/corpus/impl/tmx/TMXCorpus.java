package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.io.FileProxy;
import eu.modernmt.model.corpus.impl.BaseMultilingualCorpus;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by davide on 14/03/16.
 */
public class TMXCorpus extends BaseMultilingualCorpus {

    public static final String TMX_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'";
    public static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

    private final FileProxy tmx;
    private final String name;

    public TMXCorpus(File tmx) {
        this(FileProxy.wrap(tmx));
    }

    public TMXCorpus(FileProxy tmx) {
        this(FilenameUtils.removeExtension(tmx.getFilename()), tmx);
    }

    public TMXCorpus(String name, File tmx) {
        this(name, FileProxy.wrap(tmx));
    }

    public TMXCorpus(String name, FileProxy tmx) {
        this.name = name;
        this.tmx = tmx;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MultilingualLineReader getContentReader() throws IOException {
        return new TMXLineReader(tmx);
    }

    @Override
    public MultilingualLineWriter getContentWriter(boolean append) throws IOException {
        return new TMXLineWriter(tmx);
    }

    @Override
    public String toString() {
        return name + ".tmx";
    }

}
