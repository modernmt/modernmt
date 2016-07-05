package eu.modernmt.model.impl.ebay4cb;

import eu.modernmt.io.LineReader;
import eu.modernmt.xml.XMLUtils;
import org.apache.commons.io.IOUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Stack;

/**
 * Created by davide on 04/07/16.
 */
class Ebay4CBFileReader implements LineReader {

    private final FileInputStream stream;
    private final XMLEventReader reader;
    private final File file;
    private final Stack<String> path;

    Ebay4CBFileReader(File file) throws IOException {
        this.file = file;

        FileInputStream stream = null;
        XMLEventReader reader = null;

        try {
            stream = new FileInputStream(file);
            reader = XMLUtils.createEventReader(stream);
        } catch (XMLStreamException e) {
            throw new IOException("Error while creating XMLStreamReader for 4CB " + file, e);
        } finally {
            if (reader == null)
                IOUtils.closeQuietly(stream);
        }

        this.stream = stream;
        this.reader = reader;
        this.path = new Stack<>();
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
                            return readContentElement(reader, element, path);
                        } else {
                            path.push(getContainerId(element, name));
                        }

                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        path.pop();
                        break;
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException("Invalid 4CB " + file, e);
        }

        return null;
    }

    private static Line4CB readContentElement(XMLEventReader reader, StartElement element, Stack<String> stack) throws XMLStreamException {
        String line = XMLUtils.getXMLContent(reader, element, false);

        StringBuilder path = new StringBuilder();
        for (String container : stack) {
            path.append(container);
            path.append(',');
        }
        path.append(getContainerId(element, "ContentElement"));

        return new Line4CB(path.toString(), line);
    }

    private static String getContainerId(StartElement element, String name) throws XMLStreamException {
        if (name == null)
            name = XMLUtils.getLocalName(element);

        if ("ContentBundle".equals(name))
            return "";

        String id = XMLUtils.getAttributeValue(element, null, "id");
        String target = XMLUtils.getAttributeValue(element, null, "target");

        if (id == null)
            throw new XMLStreamException("Invalid container found: expected id for element " + name);

        StringBuilder builder = new StringBuilder(name);
        builder.append('[');
        builder.append(id);
        if (target != null) {
            builder.append('#');
            builder.append(target);
        }
        builder.append(']');

        return builder.toString();
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
        public final String path;

        private Line4CB(String path, String line) {
            this.line = line;
            this.path = path;
        }
    }

}
