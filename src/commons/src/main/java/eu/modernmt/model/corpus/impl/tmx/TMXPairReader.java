package eu.modernmt.model.corpus.impl.tmx;

import eu.modernmt.lang.LanguagePair;
import eu.modernmt.model.corpus.MultilingualCorpus;
import eu.modernmt.xml.XMLUtils;

import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by davide on 14/03/16.
 */
class TMXPairReader {

    private final LanguageCache languageCache = new LanguageCache();
    private final ArrayList<MultilingualCorpus.StringPair> resultCache = new ArrayList<>(8);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(TMXCorpus.TMX_DATE_FORMAT);
    private boolean decodeSegments = true;
    private String headerSourceLanguage = null;

    TMXPairReader() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public List<MultilingualCorpus.StringPair> read(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();
                    String name = XMLUtils.getLocalName(element);
                    if ("header".equals(name)) {
                        readHeader(element);
                    } else if ("tu".equals(name)) {
                        return readTu(reader, element);
                    }

                    break;
            }
        }

        return null;
    }

    private void readHeader(StartElement header) throws XMLStreamException {
        this.headerSourceLanguage = XMLUtils.getAttributeValue(header, null, "srclang");

        String datatype = XMLUtils.getAttributeValue(header, null, "datatype");
        datatype = datatype == null ? "unknown" : datatype.toLowerCase();

        if ("xml".equals(datatype))
            decodeSegments = false;
    }

    private List<MultilingualCorpus.StringPair> readTu(XMLEventReader reader, StartElement tu) throws XMLStreamException {
        this.resultCache.clear();

        Date tuTimestamp = getTimestamp(tu);
        String tuSourceLanguage = XMLUtils.getAttributeValue(tu, null, "srclang");

        String sourceLanguage = tuSourceLanguage == null ? headerSourceLanguage : tuSourceLanguage;
        String sourceText = null;

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            switch (event.getEventType()) {
                case XMLStreamConstants.START_ELEMENT:
                    StartElement element = event.asStartElement();

                    if ("tuv".equals(XMLUtils.getLocalName(element))) {
                        String lang = XMLUtils.getAttributeValue(element, TMXCorpus.XML_NAMESPACE, "lang");
                        if (lang == null)
                            lang = XMLUtils.getAttributeValue(element, null, "lang");
                        if (lang == null)
                            throw new XMLStreamException(format("Missing language for 'tuv'", event));

                        if (sourceLanguage == null)
                            sourceLanguage = lang; // The first <TUV> element in a <TU> is expected to be the source.

                        Date tuvTimestamp = getTimestamp(element);

                        Date timestamp = tuvTimestamp == null ? tuTimestamp : tuvTimestamp;
                        String text = readTuv(reader, element);

                        if (lang.equals(sourceLanguage)) {
                            sourceText = text;
                        } else {
                            LanguagePair language = languageCache.get(sourceLanguage, lang);
                            resultCache.add(new MultilingualCorpus.StringPair(language, null, text, timestamp));
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if ("tu".equals(XMLUtils.getLocalName(event.asEndElement()))) {
                        if (sourceText == null)
                            throw new XMLStreamException(format("Missing source text in <tu> element", event));
                        if (resultCache.isEmpty())
                            throw new XMLStreamException(format("Not enough <tuv> elements found in <tu> element", event));

                        for (MultilingualCorpus.StringPair pair : resultCache)
                            pair.source = sourceText;

                        return resultCache;
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
                        return XMLUtils.getXMLContent(reader, element, decodeSegments)
                                .replace('\n', ' ');
                    }
                    break;
            }
        }

        throw new XMLStreamException(format("Missing 'seg' inside 'tuv' element", tuv));
    }

    private Date getTimestamp(StartElement tu) throws XMLStreamException {
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

        return timestamp;
    }

    private static String format(String message, XMLEvent event) {
        Location location = event == null ? null : event.getLocation();
        return location == null ? message : (message + " at line " + location.getLineNumber());
    }

}
