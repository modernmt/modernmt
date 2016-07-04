package eu.modernmt.model.impl.fourcb;

import eu.modernmt.constants.Const;
import eu.modernmt.io.LineReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;

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
                        String name = getLocalName(element);

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
        String id = getAttributeValue(element, null, "id");
        StringWriter buffer = new StringWriter(1024);

        StringWriter lastXMLTagWriter = new StringWriter(128);
        String pendingElementName = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isEndElement() && "ContentElement".equals(getLocalName(event.asEndElement()))) {
                return new Line4CB(buffer.toString(), id);
            }

            if (pendingElementName != null) {
                String tag = lastXMLTagWriter.toString();
                lastXMLTagWriter.getBuffer().setLength(0);

                if (event.isEndElement() && pendingElementName.equals(getLocalName(event.asEndElement()))) {
                    tag = tag.substring(0, tag.length() - 1) + "/>";
                    buffer.append(tag);
                    continue;
                } else {
                    buffer.append(tag);
                }
            }

            if (event.isStartElement()) {
                event.writeAsEncodedUnicode(lastXMLTagWriter);
                pendingElementName = getLocalName(event.asStartElement());
            } else {
                event.writeAsEncodedUnicode(buffer);
            }
        }

        throw new XMLStreamException(format("Missing closing tag for 'ContentElement' element", element));
    }

    @Override
    public String readLine() throws IOException {
        return readLineWithMetadata().line;
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

        public Line4CB(String line, String id) {
            this.line = line;
            this.id = id;
        }
    }

    // Utils

    private static final String format(String message, XMLEvent event) {
        Location location = event == null ? null : event.getLocation();
        return location == null ? message : (message + " at line " + location.getLineNumber());
    }

    private static final String getLocalName(StartElement element) {
        return element.getName().getLocalPart();
    }

    private static final String getLocalName(EndElement element) {
        return element.getName().getLocalPart();
    }

    private static final String getAttributeValue(StartElement element, String namespaceURI, String localPart) {
        QName name = new QName(namespaceURI == null ? XMLConstants.NULL_NS_URI : namespaceURI, localPart);
        Attribute attribute = element.getAttributeByName(name);
        return attribute == null ? null : attribute.getValue();
    }
}
