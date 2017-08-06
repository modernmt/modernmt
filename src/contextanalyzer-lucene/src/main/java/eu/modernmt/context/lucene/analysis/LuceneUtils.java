package eu.modernmt.context.lucene.analysis;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by davide on 06/08/17.
 */
public class LuceneUtils {

    public static HashMap<String, Float> getTermFrequencies(IndexReader reader, int docId, String fieldName) throws IOException {
        Terms vector = reader.getTermVector(docId, fieldName);
        TermsEnum termsEnum = vector.iterator(null);
        HashMap<String, Float> frequencies = new HashMap<>();

        BytesRef text;
        while ((text = termsEnum.next()) != null) {
            String term = text.utf8ToString();

            float f = 0;
            DocsEnum docsEnum = termsEnum.docs(null, null);
            if (docsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS)
                f = docsEnum.freq();

            if (f > 0)
                frequencies.put(term, f);
        }

        return frequencies;
    }

    public static HashMap<String, Float> getTermFrequencies(Analyzer analyzer, Document document, String fieldName) throws IOException {
        Directory directory = new RAMDirectory();
        IndexWriter writer = null;
        IndexReader reader = null;

        try {
            // Writing document in RAM
            IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, analyzer);
            indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            writer = new IndexWriter(directory, indexConfig);
            writer.addDocument(document);
            IOUtils.closeQuietly(writer);


            reader = DirectoryReader.open(directory);
            return getTermFrequencies(reader, 0, fieldName);
        } finally {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(directory);
        }
    }

}
