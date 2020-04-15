package eu.modernmt.processing.tags.cli;

import eu.modernmt.io.*;
import eu.modernmt.model.*;
import eu.modernmt.processing.tags.projection.TagProjector;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;

public class XMLProjectorTestMain {

    private static final TagProjector projector = new TagProjector();

    public static void main(String[] args) throws Throwable {
        if (args.length != 3)
            throw new IllegalArgumentException("USAGE: source_file target_file alignment_file");

        File source = new File(args[0]);
        File target = new File(args[1]);
        File alignment = new File(args[2]);

        UnixLineWriter output = new UnixLineWriter(System.out, UTF8Charset.get());

        try (TranslationProvider provider = new TranslationProvider(source, target, alignment)) {
            Translation translation;
            while ((translation = provider.next()) != null) {
                String serialized = serialize(projector.project(translation));
                output.writeLine(serialized);
            }
        }
    }

    private static String serialize(Sentence sentence) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (Token token : sentence) {
            if (!first)
                result.append(' ');

            first = false;
            result.append(token.getPlaceholder());
        }

        return result.toString();
    }

    private static class TranslationProvider implements Closeable {

        private final LineReader sources;
        private final LineReader targets;
        private final LineReader alignments;

        public TranslationProvider(File source, File target, File alignment) throws FileNotFoundException {
            boolean success = false;

            try {
                this.sources = new UnixLineReader(new FileInputStream(source), UTF8Charset.get());
                this.targets = new UnixLineReader(new FileInputStream(target), UTF8Charset.get());
                this.alignments = new UnixLineReader(new FileInputStream(alignment), UTF8Charset.get());
                success = true;
            } finally {
                if (!success)
                    close();
            }
        }

        public Translation next() throws IOException {
            String sourceLine = this.sources.readLine();
            String targetLine = this.targets.readLine();
            String alignmentLine = this.alignments.readLine();
            if (sourceLine == null && targetLine == null && alignmentLine == null)
                return null;
            if (sourceLine == null || targetLine == null || alignmentLine == null)
                throw new IOException("files not parallel");

            Sentence sentence = parseSentence(sourceLine);
            Word[] targetWords = TokensOutputStream.deserializeWords(targetLine);
            Alignment alignment = parseAlignment(alignmentLine);

            return new Translation(targetWords, sentence, alignment);
        }

        public static Sentence parseSentence(String string) {
            ArrayList<Word> words = new ArrayList<>();
            ArrayList<Tag> tags = new ArrayList<>();

            Matcher m = XMLTag.TagRegex.matcher(string);
            int last = 0;

            while (m.find()) {
                int start = m.start();
                int end = m.end();

                Tag tag = XMLTag.fromText(string.substring(start, end));

                if (start > last) {
                    words.addAll(Arrays.asList(
                            TokensOutputStream.deserializeWords(string.substring(last, start))
                    ));
                }

                tag.setPosition(words.size());
                tags.add(tag);

                last = end;
            }

            if (last < string.length() - 1) {
                words.addAll(Arrays.asList(
                        TokensOutputStream.deserializeWords(string.substring(last))
                ));
            }

            return new Sentence(words.toArray(new Word[0]), tags.toArray(new Tag[0]));
        }

        public static Alignment parseAlignment(String string) {
            string = string.trim();

            if (string.isEmpty())
                return new Alignment(new int[0], new int[0]);

            String[] parts = string.split("\\s+");
            int[] sourceIndexes = new int[parts.length];
            int[] targetIndexes = new int[parts.length];

            for (int i = 0; i < parts.length; ++i) {
                String[] st = parts[i].split("-", 2);
                sourceIndexes[i] = Integer.parseInt(st[0]);
                targetIndexes[i] = Integer.parseInt(st[1]);
            }
            return new Alignment(sourceIndexes, targetIndexes);
        }

        @Override
        public void close() {
            IOUtils.closeQuietly(sources);
            IOUtils.closeQuietly(targets);
            IOUtils.closeQuietly(alignments);
        }
    }
}
