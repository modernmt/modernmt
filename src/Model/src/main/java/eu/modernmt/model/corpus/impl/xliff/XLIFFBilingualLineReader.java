package eu.modernmt.model.corpus.impl.xliff;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.xml.XMLUtils;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Created by davide on 18/07/16.
 */
class XLIFFBilingualLineReader implements BilingualCorpus.BilingualLineReader {

    private final XLIFFPairReader xliffPairReader = new XLIFFPairReader();

    private final File xliff;
    private final FileInputStream stream;
    private final XMLEventReader reader;
    private final String sourceLanguage;
    private final String targetLanguage;

    XLIFFBilingualLineReader(File xliff, Locale sourceLanguage, Locale targetLanguage) throws IOException {
        this.xliff = xliff;
        this.sourceLanguage = sourceLanguage.getLanguage();
        this.targetLanguage = targetLanguage.getLanguage();

        FileInputStream stream = null;
        XMLEventReader reader = null;

        try {
            stream = new FileInputStream(xliff);
            reader = XMLUtils.createEventReader(stream);
        } catch (XMLStreamException e) {
            throw new IOException("Error while creating XMLStreamReader for XLIFF " + xliff, e);
        } finally {
            if (reader == null)
                IOUtils.closeQuietly(stream);
        }

        this.stream = stream;
        this.reader = reader;
    }

    @Override
    public BilingualCorpus.StringPair read() throws IOException {
        try {
            return xliffPairReader.read(reader, sourceLanguage, targetLanguage);
        } catch (XMLStreamException e) {
            throw new IOException("Invalid TMX " + xliff, e);
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
