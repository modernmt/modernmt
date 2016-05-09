package eu.modernmt.model.impl.tmx;

import eu.modernmt.constants.Const;
import eu.modernmt.model.BilingualCorpus;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

/**
 * Created by davide on 14/03/16.
 */
public class TMXBilingualStringReader implements BilingualCorpus.BilingualStringReader {

    private final TMXPairReader tmxPairReader = new TMXPairReader();

    private File tmx;
    private final FileInputStream stream;
    private final XMLEventReader reader;
    private final String sourceLanguage;
    private final String targetLanguage;

    public TMXBilingualStringReader(File tmx, Locale sourceLanguage, Locale targetLanguage) throws IOException {
        this.tmx = tmx;
        this.sourceLanguage = sourceLanguage.toLanguageTag().substring(0, 2).toLowerCase();
        this.targetLanguage = targetLanguage.toLanguageTag().substring(0, 2).toLowerCase();

        XMLInputFactory factory = XMLInputFactory.newInstance();

        FileInputStream stream = null;
        XMLEventReader reader = null;

        try {
            stream = new FileInputStream(tmx);
            reader = factory.createXMLEventReader(new InputStreamReader(stream, Const.charset.get()));
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
            return tmxPairReader.read(reader, sourceLanguage, targetLanguage);
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
            stream.close();
        }
    }

}
