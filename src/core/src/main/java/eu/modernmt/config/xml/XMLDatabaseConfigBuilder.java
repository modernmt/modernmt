package eu.modernmt.config.xml;

import eu.modernmt.config.ConfigException;
import eu.modernmt.config.DataStreamConfig;
import eu.modernmt.config.DatabaseConfig;
import org.w3c.dom.Element;

/**
 * Created by davide on 04/01/17.
 * Updated by andrearossi on 31/03/2017
 * <p>
 * This class handles the creation of a DatabaseConfig object
 * from an XML configuration object resulting from the read
 * of the engine.xconf configuration file
 */
class XMLDatabaseConfigBuilder extends XMLAbstractBuilder {

    /**
     * This constructor initializes a new XMLDatabaseConfigBuilder
     * just storing an XML element object
     * resulting from the read of the XML configuration file
     *
     * @param element the XML element read from the file
     */
    public XMLDatabaseConfigBuilder(Element element) {
        super(element);
    }

    /**
     * For each attribute in the DatabaseConfig,
     * if the xml file element contains that attribute in the node "db"
     * then write its value in the database configuration java object
     *
     * @param config the configuration java object to fill
     *               with the XML configuration file data
     * @return the configuration java object after filling it
     * with the XML configuration file data
     */
    public DatabaseConfig build(DatabaseConfig config) throws ConfigException {
        if (this.hasAttribute("enabled"))
            config.setEnabled(this.getBooleanAttribute("enabled"));
        if (this.hasAttribute("embedded"))
            config.setEmbedded(this.getBooleanAttribute("embedded"));
        if (this.hasAttribute("host"))
            config.setHost(this.getStringAttribute("host"));
        if (this.hasAttribute("port"))
            config.setPort(this.getIntAttribute("port"));
        if (this.hasAttribute("name"))
            config.setName(this.getStringAttribute("name"));
        return config;
    }
}
