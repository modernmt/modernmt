package eu.modernmt.test.contextanalyzer.context;

import eu.modernmt.model.FileCorpus;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by lucamastrostefano on 17/02/16.
 */
public class ContextReader implements Iterator<String>{

    private BufferedReader bufferedReader;
    private int contextLinesCount;
    private LinkedList<String> lineBuffer;
    private StringBuilder context;

    public ContextReader(FileCorpus fileCorpus, int contextLinesCount) throws FileNotFoundException {
        this.bufferedReader = new BufferedReader(fileCorpus.getContentReader());
        if(contextLinesCount < 1){
            throw new RuntimeException("The parameter contextLinesCount must be greater than zero");
        }
        this.contextLinesCount = contextLinesCount;
        this.lineBuffer = new LinkedList<String>();
        this.context = new StringBuilder();
    }

    @Override
    public synchronized boolean hasNext() {
        try {
            return this.bufferedReader.ready();
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public synchronized String next() {
        try {
            if(!this.bufferedReader.ready()){
                throw new RuntimeException("No more elements to read");
            }
            this.lineBuffer.pollFirst();
            while(this.lineBuffer.size() < this.contextLinesCount && this.bufferedReader.ready()){
                String line = this.bufferedReader.readLine();
                this.lineBuffer.addLast(line);
            }
            this.context.setLength(0);
            for(String line : this.lineBuffer){
                this.context.append(line + "\n");
            }
            return this.context.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void close() throws IOException {
        this.bufferedReader.close();
    }

}
