package io.chocorean.authmod.datasource;

import io.chocorean.authmod.PlayerFactory;
import io.chocorean.authmod.guard.datasource.DatabaseSourceStrategy;
import io.chocorean.authmod.guard.datasource.IDataSourceStrategy;
import io.chocorean.authmod.guard.datasource.db.ConnectionFactory;
import io.chocorean.authmod.guard.datasource.db.IConnectionFactory;
import io.chocorean.authmod.exception.AuthmodException;
import io.chocorean.authmod.exception.PlayerAlreadyExistException;
import io.chocorean.authmod.exception.RegistrationException;
import io.chocorean.authmod.model.IPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseSourceStrategyTest {

    private static File dataFile;
    private IPlayer player;
    private IDataSourceStrategy dataSource;
    private static IConnectionFactory connectionFactory;

    @BeforeEach
    void setupConnection() throws SQLException {
        dataFile = Paths.get(System.getProperty("java.io.tmpdir"), "authmod.db").toFile();
        if(dataFile.exists()) {
            dataFile.delete();
        }
        connectionFactory = new ConnectionFactory("jdbc:sqlite:" + dataFile.getAbsolutePath());
        Connection connection = connectionFactory.getConnection();
        if(connection != null) {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate("CREATE TABLE players (" +
                    "id integer PRIMARY KEY," +
                    "email varchar(255) NOT NULL," +
                    "password varchar(255)," +
                    "uuid varchar(255), \n" +
                    "username varchar(255) NOT NULL," +
                    "banned INTEGER DEFAULT 0," +
                    "UNIQUE (email)," +
                    "UNIQUE (uuid)," +
                    "UNIQUE (username)" +
                    ");");
        }
    }

    private boolean registerPlayer() throws SQLException, RegistrationException {
        this.dataSource = new DatabaseSourceStrategy(connectionFactory);
        this.player = PlayerFactory.create();
        return dataSource.add(this.player);
    }

    @Test
    public void testConstructor() throws SQLException {
        new DatabaseSourceStrategy(connectionFactory);
    }

    @Test
    public void testConstructorTableIsDifferent() throws SQLException {
        dataFile.delete();
        Statement stmt = connectionFactory.getConnection().createStatement();
        stmt.executeUpdate("CREATE TABLE players (" +
                "id integer PRIMARY KEY," +
                "email varchar(255) NOT NULL" +
                ");");
        assertThrows(SQLException.class, () -> new DatabaseSourceStrategy(connectionFactory));
    }

    @Test
    public void testAdd() throws AuthmodException, SQLException {
        boolean added = this.registerPlayer();
        assertTrue(added, "The player should be registered");
    }

    @Test
    public void testAddDoublon() throws AuthmodException, SQLException {
        boolean added = this.registerPlayer();
        assertTrue(added, "The player should be registered");
        assertThrows(PlayerAlreadyExistException.class, this::registerPlayer);
    }

    @Test
    public void testAddSQLError() throws AuthmodException, SQLException {
        this.dataSource = new DatabaseSourceStrategy(connectionFactory);
        this.player = PlayerFactory.create();
        dataFile.delete();
        boolean added = dataSource.add(this.player);
        assertFalse(added, "The player should not be registered");
    }

    @Test
    public void testFindByEmail() throws RegistrationException, SQLException {
        this.registerPlayer();
            assertNotNull(dataSource.find(this.player.getEmail(), null), "The player should exist and be found");
    }

    @Test
    public void testFindByEmailSQLError() throws RegistrationException, SQLException {
        this.dataSource = new DatabaseSourceStrategy(connectionFactory);
        this.player = PlayerFactory.create();
        dataFile.delete();
        assertNull(dataSource.find(this.player.getEmail(), null), "The player should not exist");
    }

    @Test
    public void testFindByUsername() throws RegistrationException, SQLException {
        this.registerPlayer();
        assertNotNull(dataSource.find(null, player.getUsername()), "The player should exist and be found");
    }

    @Test
    public void testFindNotExist() throws RegistrationException, SQLException {
        this.registerPlayer();
        assertNull(dataSource.find("test@test.fr", "test"), "The player should not exist");
    }

    @Test
    public void testFindByUsernameOrEmail() throws RegistrationException, SQLException {
        this.registerPlayer();
        assertNotNull(dataSource.find(player.getEmail(), player.getUsername()), "The player should exist and be found");
    }

    @Test
    public void testFindNullParams() throws AuthmodException, SQLException {
        this.registerPlayer();
        assertNull(dataSource.find(null, null), "It should return null");
    }

}
