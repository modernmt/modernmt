package eu.modernmt.config.xml;

import eu.modernmt.lang.Language;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by davide on 04/01/17.
 */
abstract class XMLAbstractBuilder {

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

    protected static String getStringAttribute(Element element, String attr) {
        if (element == null)
            return null;

        String value = element.getAttribute(attr);
        if (value == null || value.isEmpty())
            return null;

        return value;
    }

    protected String getStringAttribute(String attr) {
        return getStringAttribute(element, attr);
    }

    protected static boolean getBooleanAttribute(Element element, String attr) {
        String value = getStringAttribute(element, attr);
        return value != null && Boolean.parseBoolean(value);
    }

    protected boolean getBooleanAttribute(String attr) {
        return getBooleanAttribute(element, attr);
    }

    protected static <E extends Enum<E>> E getEnumAttribute(Element element, String attr, Class<E> enumClass) {
        String value = getStringAttribute(element, attr);
        return value == null ? null : Enum.valueOf(enumClass, value.toUpperCase());
    }

    protected <E extends Enum<E>> E getEnumAttribute(String attr, Class<E> enumClass) {
        return getEnumAttribute(element, attr, enumClass);
    }

    protected static int getIntAttribute(Element element, String attr) {
        String value = getStringAttribute(element, attr);
        return value == null ? 0 : Integer.parseInt(value);
    }

    protected int getIntAttribute(String attr) {
        return getIntAttribute(element, attr);
    }

    protected static Language getLocaleAttribute(Element element, String attr) {
        String value = getStringAttribute(element, attr);
        return value == null ? null : Language.fromString(value);
    }

    protected Language getLocaleAttribute(String attr) {
        return getLocaleAttribute(element, attr);
    }

    protected static int[] getIntArrayAttribute(Element element, String attr) {
        String value = getStringAttribute(element, attr);
        if (value == null)
            return null;

        if (value.trim().equalsIgnoreCase("none"))
            return null;

        String[] parts = value.split("[,\\s]+");
        if (parts.length == 0)
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
