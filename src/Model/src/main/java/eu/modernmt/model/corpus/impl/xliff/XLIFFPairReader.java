package eu.modernmt.model.corpus.impl.xliff;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.xml.XMLUtils;

import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Created by davide on 18/07/16.
 */
class XLIFFPairReader {

    private BilingualCorpus.StringPair wrap(XMLEvent event, String source, String target) throws XMLStreamException {
        if (source == null)
            throw new XMLStreamException(format("Missing source sentence", event));

        if (target == null)
            throw new XMLStreamException(format("Missing target sentence", event));

        return new BilingualCorpus.StringPair(source.replace('\n', ' '), target.replace('\n', ' '));
    }

    public BilingualCorpus.StringPair read(XMLEventReader reader, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();
                    String name = XMLUtils.getLocalName(element);
                    if ("trans-unit".equals(name)) {
                        return readTranslationUnit(reader, element, sourceLanguage, targetLanguage);
                    }

                    break;
            }
        }

        return null;
    }

    private BilingualCorpus.StringPair readTranslationUnit(XMLEventReader reader, StartElement tu, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        String source = null;
        String target = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();
                    String name = XMLUtils.getLocalName(element);

                    if ("source".equals(name)) {
                        source = XMLUtils.getXMLContent(reader, element, false);
                    } else if ("target".equals(name)) {
                        target = XMLUtils.getXMLContent(reader, element, false);
                    }
                    
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("trans-unit".equals(XMLUtils.getLocalName(event.asEndElement()))) {
                        return wrap(event, source, target);
                    }
                    break;
            }
        }

        throw new XMLStreamException(format("Missing closing tag for 'trans-unit' element", tu));
    }

    private static final String format(String message, XMLEvent event) {
        Location location = event == null ? null : event.getLocation();
        return location == null ? message : (message + " at line " + location.getLineNumber());
    }

}
