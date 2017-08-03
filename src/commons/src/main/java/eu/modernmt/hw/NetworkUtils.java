package eu.modernmt.hw;

import eu.modernmt.io.DefaultCharset;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by davide on 10/04/17.
 */
public class NetworkUtils {

    /**
     * This method gets the first IPV4 address
     * that this machine is working on.
     */
    public static String getMyIpv4Address() throws UnknownHostException {
        return Inet4Address.getLocalHost().getHostAddress();
    }

    public static boolean isAvailable(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;

        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            // Nothing to do
        } finally {
            IOUtils.closeQuietly(ds);
            IOUtils.closeQuietly(ss);
        }

        return false;
    }

    public static int getAvailablePort() throws IOException {
        ServerSocket ss = null;

        try {
            ss = new ServerSocket(0);
            return ss.getLocalPort();
        } finally {
            IOUtils.closeQuietly(ss);
        }
    }

    public static boolean isLocalhost(String host) {
        InetAddress address;

        // convert the host name or ip string to an InetAddress object
        // If the address is unknown it is not localhost
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return false;
        }
        // Check if the address is a valid special local or loop back
        if (address.isAnyLocalAddress() || address.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(address) != null;
        } catch (SocketException e) {
            return false;
        }
    }


    public static NetPipe netcat(String host, int port) throws IOException {
        return new NetPipe(host, port);
    }

    public static final class NetPipe implements Closeable {

        private final Socket socket;
        private final OutputStream output;
        private final BufferedReader input;

        public NetPipe(String host, int port) throws IOException {
            this.socket = new Socket(host, port);
            this.output = socket.getOutputStream();
            this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }

        public void send(String message) throws IOException {
            this.output.write(message.getBytes(DefaultCharset.get()));
            this.output.flush();
        }

        public String receive(int timeout, TimeUnit unit) throws IOException {
            this.socket.setSoTimeout((int) unit.toMillis(timeout));
            return this.input.readLine();
        }
    }
}
