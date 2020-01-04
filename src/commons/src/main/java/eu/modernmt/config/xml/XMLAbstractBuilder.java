package eu.modernmt.config.xml;

import eu.modernmt.lang.Language;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.regex.Pattern;

/**
 * Created by davide on 04/01/17.
 */
abstract class XMLAbstractBuilder {

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{[A-Za-z._]+}");
    protected final Element element;

    public XMLAbstractBuilder(Element element) {
        this.element = element;
    }

    protected static Element getChild(Element element, String childName) {
        if (element == null)
            return null;

        NodeList children = element.getElementsByTagName(childName);
        if (children.getLength() == 0)
            return null;

        Node child = children.item(0);
        return child instanceof Element ? (Element) child : null;
    }

    protected Element getChild(String childName) {
        return getChild(element, childName);
    }

    protected static boolean hasChild(Element element, String childName) {
        return getChild(element, childName) != null;
    }

    protected boolean hasChild(String childName) {
        return hasChild(element, childName);
    }

    protected static Element[] getChildren(Element element, String name) {
        if (element == null)
            return null;

        NodeList children = element.getElementsByTagName(name);
        if (children.getLength() == 0)
            return null;

        Element[] result = new Element[children.getLength()];
        for (int i = 0; i < result.length; i++) {
            Node child = children.item(i);
            result[i] = child instanceof Element ? (Element) child : null;
        }

        return result;
    }

    protected Element[] getChildren(String name) {
        return getChildren(element, name);
    }

    protected static boolean hasAttribute(Element element, String attr) {
        if (element == null)
            return false;

        String value = element.getAttribute(attr);
        return value != null && !value.isEmpty();
    }

    protected boolean hasAttribute(String attr) {
        return hasAttribute(element, attr);
    }

    private static String getAttribute(Element element, String attr) {
        if (element == null)
            return null;

        String value = element.getAttribute(attr);
        if (value == null)
            return null;

        if (ENV_VAR_PATTERN.matcher(value).matches()) {
            String key = value.substring(2, value.length() - 1);
            value = System.getProperty(key);

            if (value == null)
                value = System.getenv(key);

            if (value == null)
                throw new IllegalArgumentException("Undefined environment variable: " + key);
        }

        value = value.trim();
        if (value.isEmpty())
            return null;

        return value;
    }

    protected static String getStringAttribute(Element element, String attr) {
        return getAttribute(element, attr);
    }

    protected String getStringAttribute(String attr) {
        return getStringAttribute(element, attr);
    }

    protected static boolean getBooleanAttribute(Element element, String attr) {
        return Boolean.parseBoolean(getAttribute(element, attr));
    }

    protected boolean getBooleanAttribute(String attr) {
        return getBooleanAttribute(element, attr);
    }

    protected static <E extends Enum<E>> E getEnumAttribute(Element element, String attr, Class<E> enumClass) {
        String value = getAttribute(element, attr);
        return value == null ? null : Enum.valueOf(enumClass, value.toUpperCase());
    }

    protected <E extends Enum<E>> E getEnumAttribute(String attr, Class<E> enumClass) {
        return getEnumAttribute(element, attr, enumClass);
    }

    protected static int getIntAttribute(Element element, String attr) {
        String value = getAttribute(element, attr);
        return value == null ? 0 : Integer.parseInt(value);
    }

    protected int getIntAttribute(String attr) {
        return getIntAttribute(element, attr);
    }

    protected static long getLongAttribute(Element element, String attr) {
        String value = getAttribute(element, attr);
        return value == null ? 0L : Long.parseLong(value);
    }

    protected long getLongAttribute(String attr) {
        return getLongAttribute(element, attr);
    }

    protected static Language getLanguageAttribute(Element element, String attr) {
        String value = getAttribute(element, attr);
        return value == null ? null : Language.fromString(value);
    }

    protected Language getLanguageAttribute(String attr) {
        return getLanguageAttribute(element, attr);
    }

    protected static String[] getStringArrayAttribute(Element element, String attr) {
        String value = getAttribute(element, attr);
        if (value == null)
            return null;

        if (value.equalsIgnoreCase("none"))
            return null;

        String[] array = value.split("[,\\s]+");
        if (array.length == 0)
            return null;

        return array;
    }

    protected String[] getStringArrayAttribute(String attr) {
        return getStringArrayAttribute(element, attr);
    }

    protected static int[] getIntArrayAttribute(Element element, String attr) {
        String[] parts = getStringArrayAttribute(element, attr);
        if (parts == null)
            return null;

        int[] array = new int[parts.length];

        for (int i = 0; i < array.length; i++)
            array[i] = Integer.parseInt(parts[i]);

        return array;
    }

    protected int[] getIntArrayAttribute(String attr) {
        return getIntArrayAttribute(element, attr);
    }

}
