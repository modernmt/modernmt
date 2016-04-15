package eu.modernmt.model.impl.tmx;

import eu.modernmt.model.BilingualCorpus;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by davide on 14/03/16.
 */
class TMXPairReader {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd'T'HHmmss'Z'");
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    private boolean decodeSegments = true;

    private BilingualCorpus.StringPair wrap(XMLEvent event, String source, String target, Date timestamp) throws XMLStreamException {
        if (source == null)
            throw new XMLStreamException(format("Missing source sentence", event));

        if (target == null)
            throw new XMLStreamException(format("Missing target sentence", event));

        return new BilingualCorpus.StringPair(source.replace('\n', ' '), target.replace('\n', ' '), timestamp);
    }

    public BilingualCorpus.StringPair read(XMLEventReader reader, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();
                    String name = getLocalName(element);
                    if ("header".equals(name)) {
                        readHeader(reader, element);
                    } else if ("tu".equals(name)) {
                        return readTu(reader, element, sourceLanguage, targetLanguage);
                    }

                    break;
            }
        }

        return null;
    }

    private void readHeader(XMLEventReader reader, StartElement header) throws XMLStreamException {
        String datatype = getAttributeValue(header, null, "datatype");
        datatype = datatype == null ? "unknown" : datatype.toLowerCase();

        if ("xml".equals(datatype))
            decodeSegments = false;
    }

    private BilingualCorpus.StringPair readTu(XMLEventReader reader, StartElement tu, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        Date timestamp = null;

        String date = getAttributeValue(tu, null, "changedate");
        if (date == null)
            date = getAttributeValue(tu, null, "creationdate");

        if (date != null) {
            try {
                timestamp = dateFormat.parse(date);
            } catch (ParseException | NumberFormatException e) {
                throw new XMLStreamException(format("Invalid date '" + date + "'", tu), e);
            }
        }

        String source = null;
        String target = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();

                    if ("tuv".equals(getLocalName(element))) {
                        String lang = getAttributeValue(element, XML_NAMESPACE, "lang");
                        if (lang == null)
                            lang = getAttributeValue(element, null, "lang");

                        String text = readTuv(reader, element);

                        if (lang == null) {
                            throw new XMLStreamException(format("Missing language for 'tuv'", event));
                        } else if (lang.startsWith(sourceLanguage)) {
                            source = text;
                        } else if (lang.startsWith(targetLanguage)) {
                            target = text;
                        } else {
                            throw new XMLStreamException("Invalid language code found: " + lang);
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("tu".equals(getLocalName(event.asEndElement()))) {
                        return wrap(event, source, target, timestamp);
                    }
                    break;
            }
        }

        throw new XMLStreamException(format("Missing closing tag for 'tuv' element", tu));
    }

    private String readTuv(XMLEventReader reader, StartElement tuv) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();

                    if ("seg".equals(getLocalName(element))) {
                        return readSegment(reader, element);
                    }
                    break;
            }
        }

        throw new XMLStreamException(format("Missing 'seg' inside 'tuv' element", tuv));
    }

    private String readSegment(XMLEventReader reader, StartElement seg) throws XMLStreamException {
        StringWriter buffer = new StringWriter(1024);

        StringWriter lastXMLTagWriter = new StringWriter(128);
        String pendingElementName = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isEndElement() && "seg".equals(getLocalName(event.asEndElement()))) {
                return buffer.toString();
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
                if (decodeSegments && event.isCharacters()) {
                    buffer.append(event.asCharacters().getData());
                } else {
                    event.writeAsEncodedUnicode(buffer);
                }
            }
        }

        throw new XMLStreamException(format("Missing closing tag for 'seg' element", seg));
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
