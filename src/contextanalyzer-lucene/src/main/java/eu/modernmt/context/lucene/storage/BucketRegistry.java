package eu.modernmt.context.lucene.storage;

import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.Language2;
import eu.modernmt.lang.LanguageDirection;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class BucketRegistry implements Closeable {

    private static File getBucketFolder(File path, long id) {
        File parent = new File(path, Long.toString(id % 10000L));
        return new File(parent, Long.toString(id));
    }

    private final File root;
    private final boolean maskLanguageRegion;
    private final Connection connection;
    private final HashMap<CacheKey, Bucket> cache = new HashMap<>();

    public BucketRegistry(File root, boolean maskLanguageRegion) throws IOException {
        this.root = root;
        this.maskLanguageRegion = maskLanguageRegion;

        try {
            File index = new File(root, "index");
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + index.getAbsolutePath());

            createDatabaseIfNotExists(connection);
        } catch (SQLException e) {
            throw new IOException(e);
        } catch (ClassNotFoundException e) {
            throw new Error(e);
        }
    }

    private static void createDatabaseIfNotExists(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();

        try {
            String sql = "CREATE TABLE IF NOT EXISTS buckets (" +
                    "id INTEGER, source TEXT, target TEXT, " +
                    "owner_lsb INTEGER, owner_msb INTEGER, " +
                    "size INTEGER, plain_size INTEGER, gz_size INTEGER, mark INTEGER DEFAULT 0, " +
                    "PRIMARY KEY (id, source, target))";
            statement.executeUpdate(sql);
        } finally {
            statement.close();
        }

        statement = connection.createStatement();

        try {
            String sql = "CREATE TABLE IF NOT EXISTS channels (id INTEGER PRIMARY KEY, position INTEGER)";
            statement.executeUpdate(sql);
        } finally {
            statement.close();
        }
    }

    public synchronized int count() throws IOException {
        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery("SELECT COUNT(*) FROM buckets WHERE size > 0");

            return result.next() ? result.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            close(result);
            close(statement);
        }
    }

    public synchronized Bucket get(long id, LanguageDirection language, UUID owner) throws IOException {
        CacheKey key = new CacheKey(id, language, this.maskLanguageRegion);

        try {
            return cache.computeIfAbsent(key, arg -> {
                try {
                    Bucket bucket = retrieve(arg.id, arg.language);
                    return bucket == null ? new Bucket(getBucketFolder(this.root, id), arg.id, arg.language, owner) : bucket;
                } catch (IOException e) {
                    throw new RuntimeIOException(e);
                }
            });
        } catch (RuntimeIOException e) {
            throw e.getCause();
        }
    }

    private synchronized Bucket retrieve(long id, LanguageDirection language) throws IOException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SELECT owner_lsb, owner_msb, size, plain_size, gz_size " +
                    "FROM buckets WHERE id = ? AND source = ? AND target = ?");
            statement.setLong(1, id);
            statement.setString(2, language.source.toString());
            statement.setString(3, language.target.toString());

            result = statement.executeQuery();

            if (result.next()) {
                UUID owner = getUUID(result, 1, 2);
                long size = result.getLong(3);
                long plainSize = result.getLong(4);
                long gzSize = result.getLong(5);

                return new Bucket(getBucketFolder(this.root, id), id, language, owner, plainSize, gzSize, size);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            close(result);
            close(statement);
        }
    }

    public synchronized Set<Bucket> getAll(long id) throws IOException {
        Set<Bucket> set = new HashSet<>();

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery("SELECT id, source, target, owner_lsb, owner_msb, size, plain_size, gz_size FROM buckets WHERE id = " + id);

            while (result.next())
                set.add(parseBucket(result));

            return set;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            close(result);
            close(statement);
        }
    }

    public synchronized Set<Bucket> getAll() throws IOException {
        Set<Bucket> set = new HashSet<>();

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery("SELECT id, source, target, owner_lsb, owner_msb, size, plain_size, gz_size FROM buckets");

            while (result.next())
                set.add(parseBucket(result));

            return set;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            close(result);
            close(statement);
        }
    }

    public synchronized Set<Bucket> getUpdated(long minMisalignment, int limit) throws IOException {
        HashSet<Bucket> set = new HashSet<>();

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery(
                    "SELECT id, source, target, owner_lsb, owner_msb, size, plain_size, gz_size " +
                            "FROM buckets " +
                            "WHERE mark > size OR (size - mark) >= " + minMisalignment + " " +
                            "ORDER BY ABS(mark - size) DESC " +
                            "LIMIT " + limit);

            while (result.next())
                set.add(parseBucket(result));

            return set;
        } catch (SQLException e) {
            throw new IOException(e);
        } catch (RuntimeIOException e) {
            throw e.getCause();
        } finally {
            close(result);
            close(statement);
        }
    }

    private Bucket parseBucket(ResultSet result) throws SQLException {
        long id = result.getLong(1);
        final Language2 source = Language2.fromString(result.getString(2));
        final Language2 target = Language2.fromString(result.getString(3));
        final UUID owner = getUUID(result, 4, 5);
        final long size = result.getLong(6);
        final long plainSize = result.getLong(7);
        final long gzSize = result.getLong(8);

        CacheKey key = new CacheKey(id, new LanguageDirection(source, target), this.maskLanguageRegion);
        Bucket bucket = cache.computeIfAbsent(key,
                arg -> new Bucket(getBucketFolder(root, arg.id), arg.id, arg.language, owner, plainSize, gzSize, size));

        return bucket;
    }

    public synchronized void mark(Bucket bucket, long mark) throws IOException {
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("UPDATE buckets SET mark = ? WHERE id = ? AND source = ? AND target = ?");
            statement.setLong(1, mark);
            statement.setLong(2, bucket.getId());
            statement.setString(3, bucket.getLanguage().source.toString());
            statement.setString(4, bucket.getLanguage().target.toString());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            close(statement);
        }
    }

    public synchronized Map<Short, Long> getChannels() throws IOException {
        HashMap<Short, Long> map = new HashMap<>();

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery("SELECT id, position FROM channels");

            while (result.next()) {
                Short id = result.getShort(1);
                Long position = result.getLong(2);

                map.put(id, position);
            }

            return map;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            close(result);
            close(statement);
        }
    }

    public synchronized void update(Map<Short, Long> channels, Set<Bucket> buckets) throws IOException {
        boolean success = false;

        PreparedStatement channelStatement = null;
        PreparedStatement iBucketStatement = null;
        PreparedStatement uBucketStatement = null;

        try {
            connection.setAutoCommit(false);

            channelStatement = connection.prepareStatement("INSERT OR REPLACE INTO channels(id, position) VALUES (?, ?)");
            iBucketStatement = connection.prepareStatement("INSERT INTO buckets(id, source, target, owner_lsb, owner_msb, size, plain_size, gz_size) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            uBucketStatement = connection.prepareStatement("UPDATE buckets SET size = ?, plain_size = ?, gz_size = ? WHERE id = ? AND source = ? AND target = ?");

            for (Map.Entry<Short, Long> entry : channels.entrySet()) {
                channelStatement.setShort(1, entry.getKey());
                channelStatement.setLong(2, entry.getValue());
                channelStatement.executeUpdate();
            }

            for (Bucket bucket : buckets) {
                // Create or update
                uBucketStatement.setLong(1, bucket.virtualSize);
                uBucketStatement.setLong(2, bucket.plainTextFileSize);
                uBucketStatement.setLong(3, bucket.compressedFileSize);
                uBucketStatement.setLong(4, bucket.getId());
                uBucketStatement.setString(5, bucket.getLanguage().source.toString());
                uBucketStatement.setString(6, bucket.getLanguage().target.toString());

                if (uBucketStatement.executeUpdate() == 0) {
                    UUID owner = bucket.getOwner();

                    iBucketStatement.setLong(1, bucket.getId());
                    iBucketStatement.setString(2, bucket.getLanguage().source.toString());
                    iBucketStatement.setString(3, bucket.getLanguage().target.toString());
                    iBucketStatement.setLong(4, owner == null ? 0L : owner.getLeastSignificantBits());
                    iBucketStatement.setLong(5, owner == null ? 0L : owner.getMostSignificantBits());
                    iBucketStatement.setLong(6, bucket.virtualSize);
                    iBucketStatement.setLong(7, bucket.plainTextFileSize);
                    iBucketStatement.setLong(8, bucket.compressedFileSize);

                    iBucketStatement.executeUpdate();
                }
            }

            connection.commit();
            success = true;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            finalizeTransaction(connection, success);

            close(channelStatement);
            close(iBucketStatement);
            close(uBucketStatement);
        }
    }

    private static void finalizeTransaction(Connection connection, boolean success) throws IOException {
        try {
            if (!success)
                connection.rollback();
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }

    private static void close(Statement statement) throws IOException {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }

    private static void close(ResultSet result) throws IOException {
        if (result != null) {
            try {
                result.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }

    private static UUID getUUID(ResultSet result, int lsbIndex, int msbIndex) throws SQLException {
        long lsb = result.getLong(lsbIndex);
        long msb = result.getLong(msbIndex);

        UUID uuid = null;
        if (lsb > 0 || msb > 0)
            uuid = new UUID(msb, lsb);
        return uuid;
    }

    public synchronized void clearCache() {
        this.cache.clear();
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            for (Bucket bucket : cache.values())
                bucket.getWriter().close();

            cache.clear();
        } finally {
            try {
                this.connection.close();
            } catch (SQLException e) {
                // Ignore it
            }
        }
    }

    private static class CacheKey {

        public long id;
        public LanguageDirection language;

        public CacheKey(long id, LanguageDirection language, boolean maskLanguageRegion) {
            if (maskLanguageRegion) {
                Language2 owSource = null;
                Language2 owTarget = null;

                if (language.source.getRegion() != null)
                    owSource = new Language2(language.source.getLanguage());
                if (language.target.getRegion() != null)
                    owTarget = new Language2(language.target.getLanguage());

                if (owSource != null || owTarget != null) {
                    if (owSource == null)
                        owSource = language.source;
                    if (owTarget == null)
                        owTarget = language.target;

                    language = new LanguageDirection(owSource, owTarget);
                }
            }

            this.id = id;
            this.language = language;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (id != cacheKey.id) return false;
            return language.equals(cacheKey.language);
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + language.hashCode();
            return result;
        }
    }

}
