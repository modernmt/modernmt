package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.io.FileProxy;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.xml.XMLUtils;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by davide on 14/03/16.
 */
class TMXLineReader implements MultilingualCorpus.MultilingualLineReader {

    private final TMXPairReader tmxPairReader = new TMXPairReader();

    private final FileProxy tmx;
    private final InputStream stream;
    private final XMLEventReader reader;

    private List<MultilingualCorpus.StringPair> cachedPairs = Collections.emptyList();

    TMXLineReader(FileProxy tmx) throws IOException {
        this.tmx = tmx;

        InputStream stream = null;
        XMLEventReader reader = null;

        try {
            stream = tmx.getInputStream();
            reader = XMLUtils.createEventReader(stream);
        } catch (XMLStreamException e) {
            throw new IOException("Error while creating XMLStreamReader for TMX " + tmx, e);
        } finally {
            if (reader == null)
                IOUtils.closeQuietly(stream);
        }

        this.stream = stream;
        this.reader = reader;
    }

    @Override
    public MultilingualCorpus.StringPair read() throws IOException {
        try {
            if (cachedPairs.isEmpty())
                cachedPairs = tmxPairReader.read(reader);

            return (cachedPairs == null || cachedPairs.isEmpty()) ? null : cachedPairs.remove(0);
        } catch (XMLStreamException e) {
            throw new IOException("Invalid TMX " + tmx, e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            throw new IOException("Error while closing XMLStreamReader", e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

}
