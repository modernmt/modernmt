package eu.modernmt.cluster;

import com.hazelcast.core.Member;

/**
 * Created by davide on 15/12/16.
 */
public class NodeInfo {

    static final String STATUS_ATTRIBUTE = "NodeInfo.STATUS_ATTRIBUTE";
    static final String OFFSET_ATTRIBUTE = "NodeInfo.OFFSET_ATTRIBUTE";

    public final String uuid;
    public final ClusterNode.Status status;
    public final long updatesOffset;

    private NodeInfo(String uuid, ClusterNode.Status status, long updatesOffset) {
        this.uuid = uuid;
        this.status = status;
        this.updatesOffset = updatesOffset;
    }

    static NodeInfo fromMember(Member member) {
        String uuid = member.getUuid();
        ClusterNode.Status status = ClusterNode.Status.valueOf(member.getStringAttribute(STATUS_ATTRIBUTE));
        Long offset = member.getLongAttribute(OFFSET_ATTRIBUTE);

        return new NodeInfo(uuid, status, offset == null ? 0L : offset);
    }

}
