package eu.modernmt.model;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Created by davide on 15/12/16.
 */
public class ImportJob {

    private static final short EPHEMERAL_JOB_HEADER = (short) 0x8000;

    public static ImportJob createEphemeralJob(int domain, long offset, short dataChannel) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putShort(EPHEMERAL_JOB_HEADER)
                .putShort(dataChannel)
                .putInt(domain)
                .putLong(offset)
                .rewind();

        long msbs = buffer.getLong();
        long lsbs = buffer.getLong();

        ImportJob job = new ImportJob();
        job.id = new UUID(msbs, lsbs);
        job.domain = domain;
        job.size = 1;
        job.begin = job.end = offset;
        job.dataChannel = dataChannel;

        return job;
    }

    public static ImportJob fromEphemeralUUID(UUID id) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .rewind();

        short header = buffer.getShort();
        if (header != EPHEMERAL_JOB_HEADER)
            return null;

        short dataChannel = buffer.getShort();
        int domain = buffer.getInt();
        long offset = buffer.getLong();

        ImportJob job = new ImportJob();
        job.id = id;
        job.domain = domain;
        job.size = 1;
        job.begin = job.end = offset;
        job.dataChannel = dataChannel;

        return job;
    }

    public static long getLongId(UUID id) {
        return id.getLeastSignificantBits();
    }

    private UUID id;
    private int domain;
    private int size;

    private long begin;
    private long end;
    private short dataChannel;

    private float progress;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setId(long id) {
        this.id = new UUID(0L, id);
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
