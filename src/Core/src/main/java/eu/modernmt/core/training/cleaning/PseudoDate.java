package eu.modernmt.core.training.cleaning;

import java.util.Date;

/**
 * Created by davide on 14/03/16.
 */
class PseudoDate {

    private long time;
    private int timeOffset;
    private int count = 0;

    public PseudoDate(Date date) {
        this.time = date == null ? 0L : date.getTime();
        this.timeOffset = 0;
    }

    public void registerUpdate(Date date) {
        long time = date == null ? 0L : date.getTime();

        if (this.time < time) {
            this.time = time;
            this.timeOffset = 0;
        } else if (this.time == time) {
            this.timeOffset++;
        }
    }

    public boolean match(Date date) {
        long time = date == null ? 0L : date.getTime();

        if (count > 0 && count > timeOffset) {
            // This is possible only if pseudo-date is already been used and
            // this is the first instance of that given sentence
            count = 0;
        }

        if (time == this.time) {
            if (timeOffset == 0) {
                return true;
            } else {
                // Look at the pseudo-date counter
                return count++ == timeOffset;
            }
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return Long.toString(time) + '+' + timeOffset;
    }
}
