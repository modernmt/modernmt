package eu.modernmt.model.impl.tmx;

import eu.modernmt.model.BilingualCorpus;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by davide on 14/03/16.
 */
class TMXPairReader {

    private final SimpleDateFormat dateFormat;
    private final Transformer transformer;

    public TMXPairReader() {
        dateFormat = new SimpleDateFormat("YYYYMMdd'T'HHmmss'Z'");
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new Error("Unable to initialize Transformer", e);
        }
    }

    private BilingualCorpus.StringPair wrap(XMLStreamReader reader, String source, String target, Date timestamp) throws XMLStreamException {
        if (source == null) {
            Location location = reader.getLocation();
            throw new XMLStreamException("Missing source sentence near line " + location.getLineNumber());
        }

        if (target == null) {
            Location location = reader.getLocation();
            throw new XMLStreamException("Missing target sentence near line " + location.getLineNumber());
        }

        return new BilingualCorpus.StringPair(source.replace('\n', ' '), target.replace('\n', ' '), timestamp);
    }

    public BilingualCorpus.StringPair read(XMLStreamReader reader, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();

            switch (type) {
                case XMLStreamReader.START_ELEMENT:
                    if ("tu".equals(reader.getLocalName()))
                        return readTu(reader, sourceLanguage, targetLanguage);
                    break;
            }
        }

        return null;
    }

    private BilingualCorpus.StringPair readTu(XMLStreamReader reader, String sourceLanguage, String targetLanguage) throws XMLStreamException {
        Date timestamp = null;

        String date = reader.getAttributeValue(null, "changedate");
        if (date == null)
            date = reader.getAttributeValue(null, "creationdate");

        if (date != null) {
            try {
                timestamp = dateFormat.parse(date);
            } catch (ParseException | NumberFormatException e) {
                throw new XMLStreamException("Invalid date '" + date + "' at line " + reader.getLocation().getLineNumber(), e);
            }
        }

        String source = null;
        String target = null;

        while (reader.hasNext()) {
            int type = reader.next();

            switch (type) {
                case XMLStreamReader.START_ELEMENT:
                    if ("tuv".equals(reader.getLocalName())) {
                        String lang = reader.getAttributeValue(null, "lang");
                        String text = readTuv(reader);

                        if (lang == null) {
                            throw new XMLStreamException("Missing language for 'tuv' at line " + reader.getLocation().getLineNumber());
                        } else if (lang.startsWith(sourceLanguage)) {
                            source = text;
                        } else if (lang.startsWith(targetLanguage)) {
                            target = text;
                        } else {
                            throw new XMLStreamException("Invalid language code found: " + lang);
                        }
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if ("tu".equals(reader.getLocalName())) {
                        return wrap(reader, source, target, timestamp);
                    }
                    break;
            }
        }

        throw new XMLStreamException("Missing closing tag for 'tuv' element at line " + reader.getLocation().getLineNumber());
    }

    private String readTuv(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();

            switch (type) {
                case XMLStreamReader.START_ELEMENT:
                    if ("seg".equals(reader.getLocalName())) {
                        return readSegment(reader);
                    }
                    break;
            }
        }

        throw new XMLStreamException("Missing 'seg' inside 'tuv' element at line " + reader.getLocation().getLineNumber());
    }

    private String readSegment(XMLStreamReader reader) throws XMLStreamException {
        StringWriter writer = new StringWriter();

        try {
            transformer.transform(new StAXSource(reader), new StreamResult(writer));
        } catch (TransformerException e) {
            throw new XMLStreamException("Unable to read segment at line " + reader.getLocation().getLineNumber(), e);
        }

        String text = writer.toString();
        int start = text.indexOf("<seg>");
        int end = text.lastIndexOf("</seg>");
        int empty = text.indexOf("<seg/>");

        if ((start < 0 || end < 0) && empty < 0)
            throw new XMLStreamException("Invalid segment at line " + reader.getLocation().getLineNumber());

        return empty < 0 ? text.substring(start + 5, end) : "";
    }


}
