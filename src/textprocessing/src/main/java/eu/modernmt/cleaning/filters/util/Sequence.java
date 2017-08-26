package eu.modernmt.cleaning.filters.util;

/**
 * Created by davide on 25/08/17.
 */
public class Sequence {

    private double sum;
    private double sum2;
    private long length;

    private double avg = Double.NaN;
    private double stddev = Double.NaN;

    public Sequence() {
        this(0, 0, 0);
    }

    public Sequence(long length, double sum, double sum2) {
        this.length = length;
        this.sum = sum;
        this.sum2 = sum2;
    }

    public void add(double value) {
        sum += value;
        sum2 += value * value;
        length++;

        avg = Double.NaN;
        stddev = Double.NaN;
    }

    public long length() {
        return length;
    }

    public double getAverage() {
        if (Double.isNaN(avg))
            avg = sum / length;
        return avg;
    }

    public double getStandardDeviation() {
        if (Double.isNaN(stddev))
            stddev = Math.sqrt((sum2 / length) - (avg * avg));
        return stddev;
    }

}
