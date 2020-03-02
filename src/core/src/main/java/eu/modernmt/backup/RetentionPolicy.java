package eu.modernmt.backup;

import java.util.List;
import java.util.Set;

public interface RetentionPolicy {

    Set<BackupFile> retain(List<BackupFile> backups);

}
