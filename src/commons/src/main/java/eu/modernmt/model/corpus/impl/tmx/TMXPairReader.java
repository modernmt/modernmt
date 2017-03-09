package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.model.corpus.BilingualCorpus;
import eu.modernmt.xml.XMLUtils;

import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by davide on 14/03/16.
 */
class TMXPairReader {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(TMXCorpus.TMX_DATE_FORMAT);
    private boolean decodeSegments = true;
    private Locale headerSourceLang = null;

    TMXPairReader() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private BilingualCorpus.StringPair wrap(XMLEvent event, String source, String target, Date timestamp, boolean inverted) throws XMLStreamException {
        if (source == null)
            throw new XMLStreamException(format("Missing source sentence", event));

        if (target == null)
            throw new XMLStreamException(format("Missing target sentence", event));

        return new BilingualCorpus.StringPair(source.replace('\n', ' '), target.replace('\n', ' '), timestamp, inverted);
    }

    public BilingualCorpus.StringPair read(XMLEventReader reader, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();
                    String name = XMLUtils.getLocalName(element);
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
        String srclang = XMLUtils.getAttributeValue(header, null, "srclang");
        if (srclang == null)
            throw new XMLStreamException("Missing 'srclang' in header element");
        headerSourceLang = Locale.forLanguageTag(srclang);

        String datatype = XMLUtils.getAttributeValue(header, null, "datatype");
        datatype = datatype == null ? "unknown" : datatype.toLowerCase();

        if ("xml".equals(datatype))
            decodeSegments = false;
    }

    private BilingualCorpus.StringPair readTu(XMLEventReader reader, StartElement tu, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        Date timestamp = null;

        String date = XMLUtils.getAttributeValue(tu, null, "changedate");
        if (date == null)
            date = XMLUtils.getAttributeValue(tu, null, "creationdate");

        if (date != null) {
            try {
                timestamp = dateFormat.parse(date);
            } catch (ParseException | NumberFormatException e) {
                throw new XMLStreamException(format("Invalid date '" + date + "'", tu), e);
            }
        }

        String srclangString = XMLUtils.getAttributeValue(tu, null, "srclang");
        Locale srclang = srclangString == null ? headerSourceLang : Locale.forLanguageTag(srclangString);

        boolean inverted = !srclang.getLanguage().equals(sourceLanguage);

        String source = null;
        String target = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();

                    if ("tuv".equals(XMLUtils.getLocalName(element))) {
                        String lang = XMLUtils.getAttributeValue(element, TMXCorpus.XML_NAMESPACE, "lang");
                        if (lang == null)
                            lang = XMLUtils.getAttributeValue(element, null, "lang");

                        String text = readTuv(reader, element);

                        if (lang == null)
                            throw new XMLStreamException(format("Missing language for 'tuv'", event));

                        lang = Locale.forLanguageTag(lang).getLanguage();

                        if (lang.equals(sourceLanguage)) {
                            source = text;
                        } else if (lang.equals(targetLanguage)) {
                            target = text;
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("tu".equals(XMLUtils.getLocalName(event.asEndElement()))) {
                        return wrap(event, source, target, timestamp, inverted);
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

                    if ("seg".equals(XMLUtils.getLocalName(element))) {
                        return XMLUtils.getXMLContent(reader, element, decodeSegments);
                    }
                    break;
            }
        }

        throw new XMLStreamException(format("Missing 'seg' inside 'tuv' element", tuv));
    }

    private static final String format(String message, XMLEvent event) {
        Location location = event == null ? null : event.getLocation();
        return location == null ? message : (message + " at line " + location.getLineNumber());
    }

}
