package eu.modernmt.hw;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Created by davide on 06/07/17.
 */
public class Graphics {

    private static int[] gpus = null;

    public static int[] getAvailableGPUs() throws IOException {
        if (gpus == null)
            gpus = computeAvailableGPUs();

        int[] result = new int[gpus.length];
        System.arraycopy(gpus, 0, result, 0, gpus.length);
        return result;
    }

    private static int[] computeAvailableGPUs() throws IOException {
        Process nvidia;
        try {
            nvidia = Runtime.getRuntime().exec(new String[]{"nvidia-smi", "--list-gpus"});
        } catch (IOException e) {
            return new int[0];
        }

        try {
            if (!nvidia.waitFor(10, TimeUnit.MINUTES))
                throw new IOException("Process \"nvidia-smi\" timeout");
        } catch (InterruptedException e) {
            throw new IOException("Process \"nvidia-smi\" timeout", e);
        } finally {
            if (nvidia.isAlive())
                nvidia.destroyForcibly();
        }

        final Pattern regex = Pattern.compile("^GPU [0-9]+:");
        List<String> lines = IOUtils.readLines(nvidia.getInputStream());

        int[] gpus = new int[lines.size()];
        int gpuCount = 0;

        for (String line : lines) {
            line = line.trim();

            if (regex.matcher(line).find()) {
                int index = Integer.parseInt(line.substring(4, line.indexOf(':')));
                gpus[gpuCount++] = index;
            }
        }

        if (gpuCount < gpus.length) {
            int[] result = new int[gpuCount];
            System.arraycopy(gpus, 0, result, 0, gpuCount);
            gpus = result;
        }

        return gpus;
    }

}
