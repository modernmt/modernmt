package eu.modernmt.context.lucene.storage;

import eu.modernmt.io.RuntimeIOException;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguagePair;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Database implements Closeable {

    private final Connection connection;

    public Database(File path) throws IOException {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + path.getAbsolutePath());

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

    public synchronized Bucket retrieve(File folder, long id, LanguagePair language) throws IOException {
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
                long ownerLsb = result.getLong(1);
                long ownerMsb = result.getLong(2);
                long size = result.getLong(3);
                long plainSize = result.getLong(4);
                long gzSize = result.getLong(5);

                UUID owner = null;
                if (ownerLsb > 0 || ownerMsb > 0)
                    owner = new UUID(ownerMsb, ownerLsb);

                return new Bucket(folder, id, language, owner, plainSize, gzSize, size);
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

    public synchronized Set<LanguagePair> retrieveLanguages(long id) throws IOException {
        Set<LanguagePair> set = new HashSet<>();

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery("SELECT source, target FROM buckets WHERE id = " + id);

            while (result.next()) {
                Language source = Language.fromString(result.getString(1));
                Language target = Language.fromString(result.getString(2));

                set.add(new LanguagePair(source, target));
            }

            return set;
        } catch (SQLException e) {
            throw new IOException(e);
        } finally {
            close(result);
            close(statement);
        }
    }

    public synchronized Set<Bucket> retrieveUpdatedBuckets(File folder, long minMisalignment, int limit, ConcurrentHashMap<CorporaStorage.CacheKey, Bucket> buckets) throws IOException {
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

            CorporaStorage.CacheKey key = new CorporaStorage.CacheKey(0, null, false);

            while (result.next()) {
                long id = result.getLong(1);
                Language source = Language.fromString(result.getString(2));
                Language target = Language.fromString(result.getString(3));

                key.id = id;
                key.language = new LanguagePair(source, target);

                final ResultSet r = result;

                Bucket bucket = buckets.computeIfAbsent(key, k -> {
                    try {
                        long ownerLsb = r.getLong(4);
                        long ownerMsb = r.getLong(5);
                        long size = r.getLong(6);
                        long plainSize = r.getLong(7);
                        long gzSize = r.getLong(8);

                        UUID owner = null;
                        if (ownerLsb > 0 || ownerMsb > 0)
                            owner = new UUID(ownerMsb, ownerLsb);

                        return new Bucket(folder, k.id, k.language, owner, plainSize, gzSize, size);
                    } catch (SQLException e) {
                        throw new RuntimeIOException(new IOException(e));
                    }
                });

                set.add(bucket);
            }

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

    @Override
    public void close() throws IOException {
        try {
            this.connection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

}
