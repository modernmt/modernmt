package eu.modernmt.context.oracle;

import eu.modernmt.context.ContextAnalyzer;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.context.ContextAnalyzerFactory;
import eu.modernmt.io.Paths;

import java.io.File;
import java.io.IOException;

/**
 * Created by david on 09/05/16.
 */
public class OracleAnalyzerFactory extends ContextAnalyzerFactory {

    @Override
    public ContextAnalyzer create() throws ContextAnalyzerException {
        return new OracleAnalyzer();
    }

}
