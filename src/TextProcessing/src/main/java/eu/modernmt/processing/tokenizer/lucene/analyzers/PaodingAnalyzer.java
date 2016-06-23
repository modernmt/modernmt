package eu.modernmt.processing.tokenizer.lucene.analyzers;

import eu.modernmt.constants.Const;
import net.paoding.analysis.Constants;
import net.paoding.analysis.analyzer.PaodingAnalyzerBean;
import net.paoding.analysis.knife.Paoding;
import net.paoding.analysis.knife.PaodingMaker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by davide on 31/05/16.
 */
public class PaodingAnalyzer extends PaodingAnalyzerBean {

    private static String PROPERTIES_FILE_PATH = null;

    private static String getPropertiesPath() {
        if (PROPERTIES_FILE_PATH == null) {
            synchronized (PaodingAnalyzer.class) {
                if (PROPERTIES_FILE_PATH == null) {
                    File paoding = new File(Const.fs.resources, "paoding");
                    File dic = new File(paoding, "dic");

                    File propertiesFile = new File(paoding, "paoding-analysis.properties");

                    Properties properties = new Properties();
                    properties.setProperty("paoding.dic.home", dic.getAbsolutePath());
                    properties.setProperty("paoding.knife.class.letterKnife", "net.paoding.analysis.knife.LetterKnife");
                    properties.setProperty("paoding.knife.class.numberKnife", "net.paoding.analysis.knife.NumberKnife");
                    properties.setProperty("paoding.knife.class.cjkKnife", "net.paoding.analysis.knife.CJKKnife");

                    FileOutputStream stream = null;
                    try {
                        if (propertiesFile.exists() && !propertiesFile.delete())
                            throw new IOException("Unable to delete file " + propertiesFile);

                        if (!propertiesFile.createNewFile())
                            throw new IOException("Unable to newInstance file " + propertiesFile);

                        stream = new FileOutputStream(propertiesFile);
                        properties.store(stream, null);
                    } catch (IOException e) {
                        throw new RuntimeException("Unable to write file " + propertiesFile, e);
                    } finally {
                        if (stream != null)
                            try {
                                stream.close();
                            } catch (IOException e) {
                            }
                    }

                    PROPERTIES_FILE_PATH = propertiesFile.getAbsolutePath();
                }
            }
        }

        return PROPERTIES_FILE_PATH;
    }

    public PaodingAnalyzer() {
        String propertiesPath = getPropertiesPath();
        Properties properties = PaodingMaker.getProperties(propertiesPath);
        String mode = Constants.getProperty(properties, "paoding.analyzer.mode");
        Paoding paoding = PaodingMaker.make(properties);
        this.setKnife(paoding);
        this.setMode(mode);
    }

}
