package eu.modernmt.api.framework;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by davide on 21/12/16.
 */
public class FileParameter {

    private final Part parameter;

    FileParameter(Part parameter) {
        this.parameter = parameter;
    }

    public String getFilename() {
        return parameter.getName();
    }

    public InputStream getInputStream() throws IOException {
        return parameter.getInputStream();
    }

    public void delete() {
        try {
            parameter.delete();
        } catch (IOException e) {
            // Ignore it
        }
    }

    @Override
    protected void finalize() throws Throwable {
        delete();
        super.finalize();
    }
}
