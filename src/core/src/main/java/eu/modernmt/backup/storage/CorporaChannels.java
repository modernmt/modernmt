package eu.modernmt.backup.storage;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class CorporaChannels {

    private final File file;
    private final Map<Short, Long> channels = new HashMap<>();

    public CorporaChannels(File file) throws IOException {
        this.file = file;

        if (file.isFile()) {
            for (String line : FileUtils.readLines(file)) {
                String[] parts = line.split(" ", 2);
                channels.put(Short.parseShort(parts[0]), Long.parseLong(parts[1]));
            }
        }
    }

    public Map<Short, Long> asMap() {
        return Collections.unmodifiableMap(channels);
    }

    public boolean skipData(short channel, long position) {
        Long existent = this.channels.get(channel);
        return existent != null && position <= existent;
    }

    public void advanceChannels(Map<Short, Long> update) throws IOException {
        Map<Short, Long> updatedChannels = new HashMap<>(channels);

        for (Map.Entry<Short, Long> entry : update.entrySet()) {
            Short channel = entry.getKey();
            Long position = entry.getValue();
            Long existent = updatedChannels.get(channel);

            if (existent == null || position > existent)
                updatedChannels.put(channel, position);
        }

        storeChannels(updatedChannels);
        channels.putAll(updatedChannels);
    }

    private void storeChannels(Map<Short, Long> channels) throws IOException {
        ArrayList<String> lines = new ArrayList<>(channels.size());

        for (Map.Entry<Short, Long> entry : channels.entrySet())
            lines.add(Short.toString(entry.getKey()) + ' ' + entry.getValue());

        FileUtils.writeLines(file, lines, false);
    }
}
