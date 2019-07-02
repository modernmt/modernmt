package eu.modernmt.backup;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BackupFile implements Comparable<BackupFile> {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

    private final Date timestamp;
    private final File path;

    public static List<BackupFile> list(File folder) {
        File[] files = folder.listFiles();
        if (files == null)
            return Collections.emptyList();

        List<BackupFile> backups = new ArrayList<>(files.length);
        for (File file : files) {
            try {
                backups.add(BackupFile.fromFile(file));
            } catch (IllegalArgumentException e) {
                // skip
            }
        }

        return backups;
    }

    public static BackupFile fromFile(File path) {
        try {
            Date date = DATE_FORMAT.parse(path.getName());
            return new BackupFile(date, path);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid backup filename: " + path.getName());
        }
    }

    public static BackupFile create(File folder) {
        Date timestamp = new Date((System.currentTimeMillis() / 1000L) * 1000L);
        String filename = DATE_FORMAT.format(timestamp);
        return new BackupFile(timestamp, new File(folder, filename));
    }

    public BackupFile(Date timestamp, File path) {
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
    public int compareTo(BackupFile o) {
        return timestamp.compareTo(o.timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BackupFile backup = (BackupFile) o;

        return path.equals(backup.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
