package eu.modernmt.contextanalyzer.lucene.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;

import java.io.Reader;

public class CorpusContentField extends Field {

	public static final FieldType TYPE_NOT_STORED = new FieldType();
	public static final FieldType TYPE_STORED = new FieldType();

	static {
		TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		TYPE_NOT_STORED.setTokenized(true);
		TYPE_NOT_STORED.setStoreTermVectors(true);
		TYPE_NOT_STORED.freeze();

		TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		TYPE_STORED.setTokenized(true);
		TYPE_STORED.setStored(true);
		TYPE_STORED.setStoreTermVectors(true);
		TYPE_STORED.freeze();
	}



	public CorpusContentField(String name, Reader reader, Store store) {
		super(name, reader, store == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
	}

	public CorpusContentField(String name, String value, Store store) {
		super(name, value, store == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
	}

	public CorpusContentField(String name, TokenStream stream) {
		super(name, stream, TYPE_NOT_STORED);
	}

}
