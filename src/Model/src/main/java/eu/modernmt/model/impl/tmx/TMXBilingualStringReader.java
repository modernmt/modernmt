package eu.modernmt.model.impl.tmx;

import eu.modernmt.config.Config;
import eu.modernmt.model.BilingualCorpus;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class TMXBilingualStringReader implements BilingualCorpus.BilingualStringReader {

    private File tmx;
    private final FileInputStream stream;
    private final XMLStreamReader reader;
    private final String sourceLanguage;
    private final String targetLanguage;

    public TMXBilingualStringReader(File tmx, Locale sourceLanguage, Locale targetLanguage) throws IOException {
        this.tmx = tmx;
        this.sourceLanguage = sourceLanguage.toLanguageTag().substring(0, 2).toLowerCase();
        this.targetLanguage = targetLanguage.toLanguageTag().substring(0, 2).toLowerCase();

        XMLInputFactory factory = XMLInputFactory.newInstance();

        FileInputStream stream = null;
        XMLStreamReader reader = null;

        try {
            stream = new FileInputStream(tmx);
            reader = factory.createXMLStreamReader(new InputStreamReader(stream, Config.charset.get()));
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
    public BilingualCorpus.StringPair read() throws IOException {
        try {
            return TMXPairReader.read(reader, sourceLanguage, targetLanguage);
        } catch (XMLStreamException e) {
            Location location = reader.getLocation();
            throw new IOException("Invalid TMX " + tmx + ": line " + location.getLineNumber() + ", column " + location.getColumnNumber(), e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            reader.close();
        } catch (XMLStreamException e) {
            throw new IOException("Error while closing XMLStreamReader", e);
        } finally {
            stream.close();
        }
    }

}
