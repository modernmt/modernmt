package eu.modernmt.decoder.moses;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by davide on 02/12/15.
 */
class MosesINI {

    private File file;
    private ArrayList<Section> sections = new ArrayList<>();

    private static class Section {
        public String name;
        public ArrayList<String> lines;

        public Section(String name) {
            this.name = name;
            this.lines = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Section section = (Section) o;

            return name.equals(section.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    public static MosesINI load(File file) throws IOException {
        MosesINI mosesINI = new MosesINI(file);

        Section header = new Section("");
        Section section = null;

        LineIterator lines = FileUtils.lineIterator(file, "UTF-8");
        while (lines.hasNext()) {
            String line = lines.nextLine().trim();
            if (line.isEmpty())
                continue;

            if (line.charAt(0) == '[' && line.charAt(line.length() - 1) == ']') {
                section = new Section(line.substring(1, line.length() - 1));
                mosesINI.sections.add(section);
            } else {
                Section dest = section == null ? header : section;
                dest.lines.add(line);
            }
        }

        if (!header.lines.isEmpty())
            mosesINI.sections.add(0, header);

        return mosesINI;
    }

    private MosesINI(File file) {
        this.file = file;
    }

    public void save() throws IOException {
        FileWriter writer = null;

        try {
            writer = new FileWriter(file, false);

            for (Section section : sections) {
                if (!section.name.isEmpty()) {
                    writer.append('[');
                    writer.append(section.name);
                    writer.append("]\n");
                }

                for (String line : section.lines) {
                    writer.append(line);
                    writer.append('\n');
                }
                writer.append('\n');
            }
        } finally {
            IOUtils.closeQuietly(writer);
        }
    }

    public File getFile() {
        return file;
    }

    public void updateWeights(Map<String, float[]> weights) {
        Section section = null;
        for (Section s : sections) {
            if ("weight".equals(s.name)) {
                section = s;
                break;
            }
        }

        if (section == null) {
            section = new Section("weight");
            sections.add(section);
        }

        section.lines.clear();

        for (Map.Entry<String, float[]> entry : weights.entrySet()) {

            StringBuilder line = new StringBuilder();
            line.append(entry.getKey());

            for (float score : entry.getValue()) {
                line.append(' ');
                if (score == MosesFeature.UNTUNEABLE_COMPONENT)
                    line.append("UNTUNEABLECOMPONENT");
                else
                    line.append(score);
            }

            section.lines.add(line.toString());
        }
    }

}
