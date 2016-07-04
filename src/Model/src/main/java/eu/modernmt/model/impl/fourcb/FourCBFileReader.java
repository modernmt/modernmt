package eu.modernmt.model.impl.fourcb;

import eu.modernmt.constants.Const;
import eu.modernmt.io.LineReader;
import eu.modernmt.xml.XMLUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by davide on 04/07/16.
 */
class FourCBFileReader implements LineReader {

    private final FileInputStream stream;
    private final XMLEventReader reader;
    private final File file;

    FourCBFileReader(File file) throws IOException {
        this.file = file;

        XMLInputFactory factory = XMLInputFactory.newInstance();

        FileInputStream stream = null;
        XMLEventReader reader = null;

        try {
            stream = new FileInputStream(file);
            reader = factory.createXMLEventReader(new InputStreamReader(new BOMInputStream(stream, false), Const.charset.get()));
        } catch (XMLStreamException e) {
            throw new IOException("Error while creating XMLStreamReader for 4CB " + file, e);
        } finally {
            if (reader == null)
                IOUtils.closeQuietly(stream);
        }

        this.stream = stream;
        this.reader = reader;
    }

    Line4CB readLineWithMetadata() throws IOException {
        try {
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();

                switch (event.getEventType()) {
                    case XMLStreamConstants.START_ELEMENT:
                        StartElement element = event.asStartElement();
                        String name = XMLUtils.getLocalName(element);

                        if ("ContentElement".equals(name)) {
                            return readContentElement(reader, element);
                        }

                        break;
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException("Invalid 4CB " + file, e);
        }

        return null;
    }

    private Line4CB readContentElement(XMLEventReader reader, StartElement element) throws XMLStreamException {
        String id = XMLUtils.getAttributeValue(element, null, "id");
        String line = XMLUtils.getXMLContent(reader, element, false);

        return new Line4CB(id, line);
    }

    @Override
    public String readLine() throws IOException {
        Line4CB mline = readLineWithMetadata();
        return mline == null ? null : mline.line;
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

    static final class Line4CB {
        public final String line;
        public final String id;

        private Line4CB(String id, String line) {
            this.line = line;
            this.id = id;
        }
    }

}
