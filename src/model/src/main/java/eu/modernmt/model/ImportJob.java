package eu.modernmt.model;

/**
 * Created by davide on 15/12/16.
 */
public class ImportJob {

    private int domain;
    private long begin;
    private long end;
    private float progress;

    public ImportJob(int domain) {
        this.domain = domain;
        this.begin = 0;
        this.end = 0;
        this.progress = 0.f;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public int getDomain() {
        return domain;
    }

    public void setDomain(int domain) {
        this.domain = domain;
    }

    public long getBegin() {
        return begin;
    }

    public void setBegin(long begin) {
        this.begin = begin;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}
