package eu.modernmt.model;

/**
 * Created by davide on 15/12/16.
 */
public class ImportJob {

    private long id;
    private int domain;
    private int size;
    private float progress;

    private long begin;
    private long end;
    private short dataChannel;

    public ImportJob(int domain) {
        this(0L, domain);
    }

    public ImportJob(long id, int domain) {
        this.id = id;
        this.domain = domain;
        this.begin = 0;
        this.end = 0;
        this.dataChannel = 0;
        this.progress = 0.f;
        this.size = 0;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public short getDataChannel() {
        return dataChannel;
    }

    public void setDataChannel(short dataChannel) {
        this.dataChannel = dataChannel;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
