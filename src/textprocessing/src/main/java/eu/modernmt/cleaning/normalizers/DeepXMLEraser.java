package eu.modernmt.cleaning.normalizers;

import eu.modernmt.cleaning.CorpusNormalizer;
import eu.modernmt.model.Tag;
import eu.modernmt.processing.xml.XMLCharacterEntity;

import java.util.regex.Pattern;

public class DeepXMLEraser implements CorpusNormalizer {

    private static final Pattern SPACES = Pattern.compile("\\s+");

    private static String stripXML(String line) {
        return Tag.TagRegex.matcher(line).replaceAll(" ");
    }

    private static String erase(String line) {
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
            } else {
                return unescaped;
            }

            line = unescaped;
        }
    }

    @Override
    public String normalize(String line) {
        line = erase(line);
        line = SPACES.matcher(line).replaceAll(" ");
        line = line.trim();

        return line;
    }

}
