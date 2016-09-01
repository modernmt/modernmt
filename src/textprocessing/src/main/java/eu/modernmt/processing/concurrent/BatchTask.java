package eu.modernmt.processing.concurrent;

import eu.modernmt.processing.PipelineInputStream;
import eu.modernmt.processing.PipelineOutputStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by davide on 31/05/16.
 */
class BatchTask<P, R> implements PipelineInputStream<P>, PipelineOutputStream<R> {

    private Iterator<P> input;
    private List<R> output;

    public BatchTask(Collection<P> input) {
        this.input = input.iterator();
        this.output = new ArrayList<>(input.size());
    }

    @Override
    public P read() {
        return input.hasNext() ? input.next() : null;
    }

    @Override
    public void write(R value) {
        output.add(value);
    }

    @Override
    public void close() {
    }

    public List<R> getOutput() {
        return output;
    }

}
