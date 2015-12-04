package eu.modernmt.network.uuid;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

/**
 * Created by davide on 27/11/15.
 */
public class UUIDSequence {

    public enum SequenceType {
        TRANSLATION_SESSION(0),
        DISTRIBUTED_TASK(1),
        WORKER(2),
        ;

        private short id;

        SequenceType(int id) {
            this.id = (short)id;
        }
    }

    private static final long MSB_MASK = 0xD3FEA2075FFC3813L;
    private static final long LSB_MASK = 0xCB09E66B53371BAFL;

    private static final long macAddress;

    static {
        byte[] address = null;

        try {
            InetAddress localhost = InetAddress.getLocalHost();
            NetworkInterface localNetworkInterface = NetworkInterface.getByInetAddress(localhost);
            address = localNetworkInterface.getHardwareAddress();
        } catch (UnknownHostException | SocketException e) {
            // Nothing to do
        }

        if (address == null) {
            address = new byte[6];
            new Random().nextBytes(address);
        }

        long longAddress;
        longAddress = ((long) address[0] & 0xFFL);
        longAddress += ((long) address[1] & 0xFFL) << 8;
        longAddress += ((long) address[2] & 0xFFL) << 16;
        longAddress += ((long) address[3] & 0xFFL) << 24;
        longAddress += ((long) address[4] & 0xFFL) << 32;
        longAddress += ((long) address[5] & 0xFFL) << 40;

        macAddress = longAddress;
    }

    private static final HashMap<Short, UUIDSequence> sequences = new HashMap<>();

    public static UUID next(SequenceType type) {
        return sequences.computeIfAbsent(type.id, integer -> new UUIDSequence(type)).next();
    }

    private final short id;
    private int sequence;

    private UUIDSequence(SequenceType type) {
        this.id = type.id;
        this.sequence = 0;
    }

    private UUID next() {
        long timestamp = System.currentTimeMillis() & 0xFFFFFFFFL;
        long sequenceId = id & 0xFFFF;

        long sequenceNumber;
        synchronized (this) {
            sequenceNumber = ++sequence & 0xFFFFFFFFL;
        }

        long msb = ((macAddress << 16) + sequenceId) ^ MSB_MASK;
        long lsb = ((sequenceNumber << 32) + timestamp) ^ LSB_MASK;

        return new UUID(msb, lsb);
    }
}
