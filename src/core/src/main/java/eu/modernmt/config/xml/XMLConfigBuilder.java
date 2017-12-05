package eu.modernmt.config.xml;

import eu.modernmt.config.ConfigException;
import eu.modernmt.config.NodeConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;

/**
 * Created by davide on 04/01/17.
 */
public class XMLConfigBuilder extends XMLAbstractBuilder {

    private final XMLNetworkConfigBuilder networkConfigBuilder;
    private final XMLDataStreamConfigBuilder dataStreamConfigBuilder;
    private final XMLDatabaseConfigBuilder databaseConfigBuilder;
    private final XMLEngineConfigBuilder engineConfigBuilder;
    private final XMLTranslationQueueConfigBuilder translationQueueConfigBuilder;

    private XMLConfigBuilder(Element element) {
        super(element);

        networkConfigBuilder = new XMLNetworkConfigBuilder(getChild("network"));
        dataStreamConfigBuilder = new XMLDataStreamConfigBuilder(getChild("datastream"));
        databaseConfigBuilder = new XMLDatabaseConfigBuilder(getChild("db"));
        engineConfigBuilder = new XMLEngineConfigBuilder(getChild("engine"));
        translationQueueConfigBuilder = new XMLTranslationQueueConfigBuilder(getChild("translation-queue"));
    }

    public static NodeConfig build(File file) throws ConfigException {
        Document document;

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            Source source = new StreamSource(file);

            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            document = builder.newDocument();

            Result result = new DOMResult(document);
            transformer.transform(source, result);
        } catch (TransformerException | ParserConfigurationException e) {
            throw new ConfigException("Unable to parse config file " + file, e);
        }

        XMLConfigBuilder instance = new XMLConfigBuilder(document.getDocumentElement());
        return instance.build(new NodeConfig());
    }

    private NodeConfig build(NodeConfig config) throws ConfigException {
        networkConfigBuilder.build(config.getNetworkConfig());
        dataStreamConfigBuilder.build(config.getDataStreamConfig());
        databaseConfigBuilder.build(config.getDatabaseConfig());
        engineConfigBuilder.build(config.getEngineConfig());
        translationQueueConfigBuilder.build(config.getTranslationQueueConfig());

        return config;
    }

}
