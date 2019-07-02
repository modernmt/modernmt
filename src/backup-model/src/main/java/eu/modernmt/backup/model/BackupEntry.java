package eu.modernmt.backup.model;

import eu.modernmt.lang.LanguageDirection;

import java.util.Date;
import java.util.UUID;

public class BackupEntry {

    public final UUID owner;
    public final LanguageDirection language;
    public final long memory;
    public final String sentence;
    public final String translation;
    public final Date timestamp;

    public BackupEntry(UUID owner, LanguageDirection language, long memory, String sentence, String translation, Date timestamp) {
        this.owner = owner;
        this.language = language;
        this.memory = memory;
        this.sentence = sentence;
        this.translation = translation;
        this.timestamp = timestamp;
    }
}
