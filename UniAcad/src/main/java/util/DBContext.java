package util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

public class DBContext {
    protected Connection connection;
    private static final Logger LOGGER = Logger.getLogger(DBContext.class.getName());

    public DBContext() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream("database.properties"));
            String url = properties.getProperty("DB_URL");
            String username = properties.getProperty("DB_USER");
            String password = properties.getProperty("DB_PASSWORD");
            String driver = properties.getProperty("DB_DRIVER");

            Class.forName(driver);
            connection = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            LOGGER.severe("Error initializing database connection: " + e.getMessage());
        }
    }

    protected void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.severe("Error closing database connection: " + e.getMessage());
        }
    }
}