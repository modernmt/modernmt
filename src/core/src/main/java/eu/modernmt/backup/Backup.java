package eu.modernmt.backup;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Backup implements Comparable<Backup> {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    private final Date timestamp;
    private final File path;

    public static List<Backup> list(File folder) {
        File[] files = folder.listFiles();
        if (files == null)
            return Collections.emptyList();

        List<Backup> backups = new ArrayList<>(files.length);
        for (File file : files) {
            try {
                backups.add(Backup.fromFile(file));
            } catch (IllegalArgumentException e) {
                // skip
            }
        }

        Collections.sort(backups);
        return backups;
    }

    public static Backup fromFile(File path) {
        try {
            Date date = DATE_FORMAT.parse(path.getName());
            return new Backup(date, path);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid backup filename: " + path.getName());
        }
    }

    public static Backup create(File folder) {
        Date timestamp = new Date((System.currentTimeMillis() / 1000L) * 1000L);
        String filename = DATE_FORMAT.format(timestamp);
        return new Backup(timestamp, new File(folder, filename));
    }

    public Backup(Date timestamp, File path) {
        this.timestamp = timestamp;
        this.path = path;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public File getPath() {
        return path;
    }

    @Override
    public String toString() {
        return path.getName();
    }

    @Override
    public int compareTo(Backup o) {
        return timestamp.compareTo(o.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Backup backup = (Backup) o;

        return path.equals(backup.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
