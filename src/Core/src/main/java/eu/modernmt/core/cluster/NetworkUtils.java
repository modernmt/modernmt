package eu.modernmt.core.cluster;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

/**
 * Created by davide on 27/04/16.
 */
class NetworkUtils {

    public static Collection<String> listPublicInterfaces() {
        ArrayList<String> result = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface netint = nets.nextElement();
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();

                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress())
                        result.add(inetAddress.getHostAddress());
                }
            }
        } catch (SocketException e) {
            // Ignore it
        }

        return result;
    }

}
