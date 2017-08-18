package eu.modernmt.engine;

/**
 * Created by davide on 01/08/17.
 */
public class ContributionOptions {

    public final boolean process;
    public final boolean align;

    public ContributionOptions(boolean process, boolean align) {
        this.process = process;
        this.align = align;
    }

}
