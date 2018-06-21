package eu.modernmt.cluster;

import eu.modernmt.lang.LanguagePair;

import java.util.Collection;
import java.util.Set;

/**
 * Created by davide on 15/12/16.
 */
public class ServerInfo {

    public static class ClusterInfo {

        public final Collection<NodeInfo> nodes;

        public ClusterInfo(Collection<NodeInfo> nodes) {
            this.nodes = nodes;
        }
    }

    public static class BuildInfo {

        public final String version;
        public final long number;

        public BuildInfo(String version, long number) {
            this.version = version;
            this.number = number;
        }

    }

    private final ClusterInfo cluster;
    private final BuildInfo build;
    private final Set<LanguagePair> languages;

    public ServerInfo(ClusterInfo cluster, BuildInfo build, Set<LanguagePair> languages) {
        this.cluster = cluster;
        this.build = build;
        this.languages = languages;
    }

}
