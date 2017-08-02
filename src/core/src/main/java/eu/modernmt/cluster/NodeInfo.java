package eu.modernmt.cluster;

import com.hazelcast.core.Member;
import eu.modernmt.lang.LanguagePair;

import java.util.*;

/**
 * Created by davide on 15/12/16.
 */
public class NodeInfo {

    private static final String STATUS_ATTRIBUTE = "NodeInfo.STATUS_ATTRIBUTE";
    private static final String DATA_CHANNELS_ATTRIBUTE = "NodeInfo.DATA_CHANNELS_ATTRIBUTE";
    private static final String TRANSLATION_DIRECTIONS_ATTRIBUTE = "NodeInfo.TRANSLATION_DIRECTIONS_ATTRIBUTE";

    public final String uuid;
    public final ClusterNode.Status status;
    public final Map<Short, Long> channelsPositions;
    public final Set<LanguagePair> loadedTranslationDirections;

    static NodeInfo fromMember(Member member) {
        String uuid = member.getUuid();
        ClusterNode.Status status = ClusterNode.Status.valueOf(member.getStringAttribute(STATUS_ATTRIBUTE));
        Map<Short, Long> positions = deserializeChannels(member.getStringAttribute(DATA_CHANNELS_ATTRIBUTE));
        Set<LanguagePair> loadedTranslationDirections = deserializeTranslationDirections(member.getStringAttribute(TRANSLATION_DIRECTIONS_ATTRIBUTE));

        return new NodeInfo(uuid, status, positions, loadedTranslationDirections);
    }

    private NodeInfo(String uuid, ClusterNode.Status status, Map<Short, Long> channelsPositions, Set<LanguagePair> loadedTranslationDirections) {
        this.uuid = uuid;
        this.status = status;
        this.channelsPositions = channelsPositions;
        this.loadedTranslationDirections = loadedTranslationDirections;
    }

    // Utils

    static boolean hasTranslationDirection(Member member, LanguagePair direction) {
        String encoded = member.getStringAttribute(TRANSLATION_DIRECTIONS_ATTRIBUTE);
        if (encoded == null || encoded.isEmpty())
            return false;

        String search = '[' + direction.source.toLanguageTag() + ':' + direction.target.toLanguageTag() + ']';
        return encoded.contains(search);
    }

    static void updateStatusInMember(Member member, ClusterNode.Status status) {
        member.setStringAttribute(STATUS_ATTRIBUTE, status.name());
    }

    static void updateTranslationDirections(Member member, Set<LanguagePair> directions) {
        member.setStringAttribute(TRANSLATION_DIRECTIONS_ATTRIBUTE, serialize(directions));
    }

    static void updateChannelsPositionsInMember(Member member, Map<Short, Long> update) {
        HashMap<Short, Long> positions = deserializeChannels(member.getStringAttribute(DATA_CHANNELS_ATTRIBUTE));
        for (Map.Entry<Short, Long> position : update.entrySet()) {
            positions.put(position.getKey(), position.getValue());
        }
        member.setStringAttribute(DATA_CHANNELS_ATTRIBUTE, serialize(positions));
    }

    // Serializers

    private static String serialize(Set<LanguagePair> directions) {
        if (directions == null || directions.isEmpty())
            return "";

        StringBuilder builder = new StringBuilder();

        for (LanguagePair direction : directions) {
            builder.append('[');
            builder.append(direction.source.toLanguageTag());
            builder.append(':');
            builder.append(direction.target.toLanguageTag());
            builder.append(']');
            builder.append(',');
        }

        return builder.substring(0, builder.length() - 1);
    }

    private static String serialize(Map<Short, Long> positions) {
        if (positions == null || positions.isEmpty())
            return "";

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<Short, Long> entry : positions.entrySet()) {
            builder.append(entry.getKey());
            builder.append(':');
            builder.append(entry.getValue());
            builder.append(',');
        }

        return builder.substring(0, builder.length() - 1);
    }

    // Deserializers

    private static Set<LanguagePair> deserializeTranslationDirections(String encoded) {
        if (encoded == null || encoded.isEmpty())
            return Collections.emptySet();

        String[] elements = encoded.split(",");

        HashSet<LanguagePair> result = new HashSet<>(elements.length);
        for (String element : elements) {
            String[] tags = element.split(":");

            Locale source = Locale.forLanguageTag(tags[0].substring(1, tags[0].length() - 1));
            Locale target = Locale.forLanguageTag(tags[1].substring(1, tags[1].length() - 1));

            result.add(new LanguagePair(source, target));
        }

        return result;
    }

    private static HashMap<Short, Long> deserializeChannels(String encoded) {
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
