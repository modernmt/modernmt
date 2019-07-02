package eu.modernmt.backup;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileLimitRetentionPolicy implements RetentionPolicy {

    private final int limit;

    public FileLimitRetentionPolicy(int limit) {
        this.limit = limit;
    }

    @Override
    public Set<BackupFile> retain(List<BackupFile> backups) {
        Collections.sort(backups);
        Collections.reverse(backups);

        if (backups.size() > limit)
            return new HashSet<>(backups.subList(0, limit));
        else
            return new HashSet<>(backups);
    }

}
