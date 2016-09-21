package eu.modernmt.persistence.sqlite;

import eu.modernmt.model.Domain;
import eu.modernmt.persistence.DomainDAO;
import eu.modernmt.persistence.PersistenceException;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by davide on 21/09/16.
 */
public class SQLiteDomainDAO implements DomainDAO {

    private Connection connection;

    public SQLiteDomainDAO(SQLiteConnection connection) {
        this.connection = connection.connection;
    }

    @Override
    public Domain retrieveBytId(int id) throws PersistenceException {
        PreparedStatement statement = null;
        ResultSet result = null;

        try {
            statement = connection.prepareStatement("SELECT * FROM domains WHERE \"id\" = ?");
            statement.setInt(1, id);

            result = statement.executeQuery();

            if (result.next()) {
                String name = result.getString("name");
                return new Domain(id, name);
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
    public Collection<Domain> retrieveAll() throws PersistenceException {
        ArrayList<Domain> list = new ArrayList<>();

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = connection.createStatement();
            result = statement.executeQuery("SELECT * FROM domains");

            while (result.next()) {
                int id = result.getInt("id");
                String name = result.getString("name");

                list.add(new Domain(id, name));
            }
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
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
