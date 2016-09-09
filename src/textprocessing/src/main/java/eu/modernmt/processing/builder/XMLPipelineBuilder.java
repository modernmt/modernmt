package eu.modernmt.processing.builder;

import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.ProcessingPipeline;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by davide on 31/05/16.
 */
public class XMLPipelineBuilder<P, R> extends PipelineBuilder<P, R> {

    protected XMLPipelineBuilder(List<AbstractBuilder> builders, Class<ProcessingPipeline> pipelineClass) {
        super(builders, pipelineClass);
    }

    public static <P, R> XMLPipelineBuilder<P, R> loadFromXML(File file) throws IOException, ProcessingException {
        FileInputStream input = null;

        try {
            input = new FileInputStream(file);
            return loadFromXML(input);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    @SuppressWarnings("unchecked")
    public static <P, R> XMLPipelineBuilder<P, R> loadFromXML(InputStream input) throws IOException {
        Document xml = getXMLDocument(input);
        Element pipeline = xml.getDocumentElement();

        // Pipeline Class
        Class<ProcessingPipeline> pipelineClass = ProcessingPipeline.class;
        if (pipeline.hasAttribute("class")) {
            String className = pipeline.getAttribute("class").trim();
            try {
                pipelineClass = (Class<ProcessingPipeline>) Class.forName(className);
            } catch (ClassCastException | ClassNotFoundException e) {
                throw new IOException("Invalid pipeline class " + className, e);
            }
        }

        ArrayList<AbstractBuilder> builders = new ArrayList<>();

        NodeList processors = pipeline.getChildNodes();
        for (int i = 0; i < processors.getLength(); i++) {
            Node node = processors.item(i);
            if (node instanceof Element)
                builders.add(parseNode((Element) node));
        }

        return new XMLPipelineBuilder(builders, pipelineClass);
    }

    private static Document getXMLDocument(InputStream input) throws IOException {
        String packageName = ProcessingPipeline.class.getPackage().getName().replace('.', '/');
        String xsdPath = packageName + "/pipeline-schema.xsd";

        Document xml;
        InputStream xsdResource = null;

        try {
            xsdResource = ProcessingPipeline.class.getClassLoader().getResourceAsStream(xsdPath);
            if (xsdResource == null)
                throw new IOException("Unable to load XSD " + xsdPath);

            // Load XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            xml = builder.parse(input);

            // Optional, but recommended
            // read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            xml.getDocumentElement().normalize();

            // Validate XML
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(xsdResource));
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(xml));
        } catch (ParserConfigurationException e) {
            throw new Error("Unable to instantiate XML Document Factory", e);
        } catch (SAXException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(xsdResource);
        }

        return xml;
    }

    private static AbstractBuilder parseNode(Element node) {
        String tag = node.getTagName();

        if ("processor".equals(tag)) {
            return getProcessorNode(node);
        } else if ("processorGroup".equals(tag)) {
            ArrayList<ProcessorBuilder> factories = new ArrayList<>();

            NodeList nodes = node.getElementsByTagName("processor");
            for (int i = 0; i < nodes.getLength(); i++)
                factories.add(getProcessorNode((Element) nodes.item(i)));

            return new ProcessorGroupBuilder(factories);
        }

        throw new Error("This should never happen");
    }

    private static ProcessorBuilder getProcessorNode(Element node) {
        String sourceAttribute = node.hasAttribute("source") ? node.getAttribute("source") : null;
        String targetAttribute = node.hasAttribute("target") ? node.getAttribute("target") : null;
        String className = node.getTextContent().trim();

        if (sourceAttribute == null && targetAttribute == null)
            return new ProcessorBuilder(className);
        else
            return new FilteredProcessorBuilder(className, sourceAttribute, targetAttribute);
    }


}
