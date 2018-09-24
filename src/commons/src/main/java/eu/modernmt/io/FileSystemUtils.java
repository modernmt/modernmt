package eu.modernmt.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class FileSystemUtils {

    public static void atomicWrite(File file, String content) throws IOException {
        atomicWrite(file, content.getBytes());
    }

    public static void atomicWrite(File file, byte[] content) throws IOException {
        atomicWrite(file, new ByteArrayInputStream(content));
    }

    public static void atomicWrite(File file, InputStream stream) throws IOException {
        File temp = new File(file.getParentFile(), "~" + file.getName());

        try {
            FileOutputStream output = null;

            try {
                output = new FileOutputStream(temp, false);
                IOUtils.copy(stream, output);
                fsync(output);
            } finally {
                IOUtils.closeQuietly(output);
            }

            Files.move(temp.toPath(), file.toPath(), REPLACE_EXISTING, ATOMIC_MOVE);
        } finally {
            FileUtils.deleteQuietly(temp);
        }
    }

    public static void fsync(FileOutputStream stream) throws IOException {
        stream.flush();
        stream.getFD().sync();
    }

}
