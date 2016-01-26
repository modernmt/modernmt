package eu.modernmt.processing;

import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingPipeline;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by davide on 26/01/16.
 */
public class Main {

    public static void main(String[] args) throws Throwable {
        ProcessingPipeline<String, String> pipeline = new ProcessingPipeline.Builder<String, String>()
                .setThreads(4)
                .add(new UppercaseProcessor())
                .add(new LowercaseProcessor())
                .create();

        FileInputStream input = null;

        try {
            input = new FileInputStream("/Users/davide/Documents/consistency_test-10+30D/train/taus-legal-european_commission-10599.it");
            process(pipeline, input, System.out);
        } finally {
            IOUtils.closeQuietly(pipeline);
            IOUtils.closeQuietly(input);
        }
    }

    private static void process(ProcessingPipeline<String, String> pipeline, String line) throws Exception {
        String tokens = pipeline.process(line);
        System.out.println(tokens);
    }

    private static void process(ProcessingPipeline<String, String> pipeline, InputStream input, OutputStream output) throws Exception {
        //pipeline.processAll(PipelineInputStream.fromInputStream(input), PipelineOutputStream.fromOutputStream(output));
        pipeline.processAll(PipelineInputStream.fromInputStream(input), PipelineOutputStream.blackHole());
    }

}
