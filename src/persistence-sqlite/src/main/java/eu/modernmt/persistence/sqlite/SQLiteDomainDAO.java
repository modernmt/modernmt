package eu.modernmt.persistence.sqlite;

import eu.modernmt.model.Domain;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.PersistenceException;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by davide on 21/09/16.
 */
public class SQLiteDomainDAO implements DomainDAO {

    private Connection connection;

    public SQLiteDomainDAO(SQLiteConnection connection) {
        this.connection = connection.connection;
    }

    private static Domain read(ResultSet result) throws SQLException {
        int id = result.getInt("id");
        String name = result.getString("name");

        return new Domain(id, name);
    }

    @Override
    public Domain retrieveById(int id) throws PersistenceException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SELECT * FROM domains WHERE \"id\" = ?");
            statement.setInt(1, id);

            result = statement.executeQuery();

            return result.next() ? read(result) : null;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(result);
            SQLUtils.closeQuietly(statement);
        }
    }

    @Override
    public Map<Integer, Domain> retrieveByIds(Collection<Integer> ids) throws PersistenceException {
        Map<Integer, Domain> map = new HashMap<>(ids.size());

        if (ids.isEmpty())
            return map;

        StringBuilder sql = new StringBuilder("SELECT * FROM domains WHERE \"id\" IN (");
        for (Integer id : ids) {
            sql.append(id);
            sql.append(',');
        }

        sql.setCharAt(sql.length() - 1, ')');

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery(sql.toString());

            while (result.next()) {
                Domain domain = read(result);
                map.put(domain.getId(), domain);
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(result);
            SQLUtils.closeQuietly(statement);
        }

        return map;
    }

    @Override
    public Collection<Domain> retrieveAll() throws PersistenceException {
        ArrayList<Domain> list = new ArrayList<>();

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery("SELECT * FROM domains");

            while (result.next())
                list.add(read(result));
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(result);
            SQLUtils.closeQuietly(statement);
        }

        return list;
    }

    @Override
    public Domain put(Domain domain) throws PersistenceException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("INSERT INTO domains(\"name\") VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, domain.getName());

            statement.executeUpdate();

            result = statement.getGeneratedKeys();

            if (result.next()) {
                domain.setId(result.getInt(1));
                return domain;
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(result);
            SQLUtils.closeQuietly(statement);
        }
    }

    @Override
    public Domain update(Domain domain) throws PersistenceException {
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("UPDATE domains SET \"name\"=? WHERE id = ?");
            statement.setString(1, domain.getName());
            statement.setLong(2, domain.getId());

            return statement.executeUpdate() == 1 ? domain : null;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
        }
    }

    @Override
    public boolean delete(int id) throws PersistenceException {
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement("DELETE FROM domains WHERE id = ?");
            statement.setInt(1, id);

            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PersistenceException(e);
        } finally {
            SQLUtils.closeQuietly(statement);
        }
    }

}
