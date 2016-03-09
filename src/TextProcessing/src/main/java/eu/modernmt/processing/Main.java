package eu.modernmt.processing;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Token;
import eu.modernmt.processing.framework.PipelineInputStream;
import eu.modernmt.processing.framework.PipelineOutputStream;
import eu.modernmt.processing.framework.ProcessingJob;
import eu.modernmt.processing.framework.ProcessingPipeline;
import eu.modernmt.processing.util.TokensOutputter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

/**
 * Created by davide on 08/03/16.
 */
public class Main {

    public static void main(String[] args) throws Throwable {
        System.setProperty("mmt.home", "/Users/davide/workspaces/mmt/ModernMT/");

        String sourceText = "<a> You&apos;ll see the <div id=\"example-div\"><br> example</div>! &lt;button&gt;";
        String targetText = "Vedrai l&apos;<div id=\"example-div\"><br>esempio</div>! &lt;bottone&gt;";

        Preprocessor preprocessor = new Preprocessor(Locale.ENGLISH);
        Postprocessor postprocessor = new Postprocessor(Locale.ITALIAN);

        try {
            Sentence source = preprocessor.process(sourceText, true);
            System.out.println(sourceText);
            System.out.println(source);

            for (Token token : source) {
                System.out.print(token.toString() + ' ');
            }
        } finally {
            preprocessor.close();
            postprocessor.close();
        }

        File dest = new File("/Users/davide/Desktop/tokenizer/text.test.en");

        FileUtils.deleteQuietly(dest);

        ProcessingPipeline<String, Sentence> pipeline = null;
        PipelineInputStream<String> input = null;
        PipelineOutputStream<Sentence> output = null;

        try {
            pipeline = Preprocessor.getPipeline(Locale.ENGLISH, true);
            input = PipelineInputStream.fromInputStream(new FileInputStream("/Users/davide/Desktop/tokenizer/text.en"));
            output = new TokensOutputter(new FileOutputStream(dest, false), true, false);

            ProcessingJob<String, Sentence> job = pipeline.createJob(input, output);

            job.start();
            job.join();

        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(pipeline);
        }
    }

}
