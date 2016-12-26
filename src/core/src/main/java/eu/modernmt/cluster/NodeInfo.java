package eu.modernmt.cluster;

import com.hazelcast.core.Member;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 15/12/16.
 */
public class NodeInfo {

    private static final String STATUS_ATTRIBUTE = "NodeInfo.STATUS_ATTRIBUTE";
    private static final String DATA_CHANNELS_ATTRIBUTE = "NodeInfo.DATA_CHANNELS_ATTRIBUTE";

    public final String uuid;
    public final ClusterNode.Status status;
    public final Map<Short, Long> channelsPositions;

    private NodeInfo(String uuid, ClusterNode.Status status, Map<Short, Long> channelsPositions) {
        this.uuid = uuid;
        this.status = status;
        this.channelsPositions = channelsPositions;
    }

    static NodeInfo fromMember(Member member) {
        String uuid = member.getUuid();
        ClusterNode.Status status = ClusterNode.Status.valueOf(member.getStringAttribute(STATUS_ATTRIBUTE));
        Map<Short, Long> positions = fromString(member.getStringAttribute(DATA_CHANNELS_ATTRIBUTE));

        return new NodeInfo(uuid, status, positions);
    }

    static void updateStatusInMember(Member member, ClusterNode.Status status) {
        member.setStringAttribute(STATUS_ATTRIBUTE, status.name());
    }

    static void updateChannelsPositionsInMember(Member member, Map<Short, Long> update) {
        HashMap<Short, Long> positions = fromString(member.getStringAttribute(DATA_CHANNELS_ATTRIBUTE));
        for (Map.Entry<Short, Long> position : update.entrySet()) {
            positions.put(position.getKey(), position.getValue());
        }
        member.setStringAttribute(DATA_CHANNELS_ATTRIBUTE, toString(positions));
    }

    private static String toString(Map<Short, Long> positions) {
        if (positions == null || positions.isEmpty())
            return null;

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<Short, Long> entry : positions.entrySet()) {
            builder.append(entry.getKey());
            builder.append(':');
            builder.append(entry.getValue());
            builder.append(',');
        }

        return builder.substring(0, builder.length() - 1);
    }

    private static HashMap<Short, Long> fromString(String encoded) {
        if (encoded == null || encoded.isEmpty())
            return new HashMap<>();

        String[] elements = encoded.split(",");

        HashMap<Short, Long> result = new HashMap<>(elements.length);
        for (String element : elements) {
            String[] keyvalue = element.split(":");
            result.put(Short.parseShort(keyvalue[0]), Long.parseLong(keyvalue[1]));
        }

        return result;
    }

}
