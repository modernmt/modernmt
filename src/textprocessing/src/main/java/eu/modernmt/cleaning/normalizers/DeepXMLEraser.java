package eu.modernmt.cleaning.normalizers;

import eu.modernmt.cleaning.CorpusNormalizer;
import eu.modernmt.model.Tag;
import eu.modernmt.processing.xml.XMLCharacterEntity;

public class DeepXMLEraser implements CorpusNormalizer {

    private static String stripXML(String line) {
        return Tag.TagRegex.matcher(line).replaceAll(" ");
    }

    @Override
    public String normalize(String line) {
        while (true) {
            String xmlStrip = stripXML(line);
            String unescaped = XMLCharacterEntity.unescapeAll(xmlStrip);

            // if no entities found, then break
            if (unescaped.equals(xmlStrip))
                return xmlStrip;

            if (xmlStrip.equals(line)) {
                // no XML tags found (but entities found)
                if (xmlStrip.indexOf('<') != -1 || xmlStrip.indexOf('>') != -1) {
                    // if no XML tags, but these chars found, then it must be intentional
                    return unescaped;
                }
            }

            line = unescaped;
        }
    }

}
