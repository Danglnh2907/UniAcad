package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The DBContext class provides a simple implementation for managing a database connection.
 * It reads configuration details from a 'database.properties' file and establishes a connection
 * using the standard JDBC API.
 *
 * The 'database.properties' file must include the following keys:
 * - DB_URL: The URL of the database to connect to.
 * - DB_USER: The username required for authentication.
 * - DB_PASSWORD: The password required for authentication.
 * - DB_DRIVER: The fully qualified class name of the JDBC driver.
 *
 * This class also includes logging for errors during the connection setup and teardown processes.
 */
public class DBContext {
    protected Connection connection;
    private static final Logger LOGGER = Logger.getLogger(DBContext.class.getName());

    /**
     * Initializes a new instance of the DBContext class.
     * This constructor establishes a database connection using the configuration
     * specified in the 'database.properties' file. The file must be located in the classpath.
     *
     * The database.properties file should contain the following keys:
     * - DB_URL: The JDBC URL for the database connection.
     * - DB_USER: The username for database authentication.
     * - DB_PASSWORD: The password for database authentication.
     * - DB_DRIVER: The fully qualified name of the JDBC driver class.
     *
     * The method performs the following steps:
     * 1. Loads the database properties file from the classpath.
     * 2. Validates the presence of required configuration keys.
     * 3. Loads the JDBC driver class via Class.forName().
     * 4. Establishes a connection to the database using DriverManager.
     *
     * Logging is performed for various error cases, such as:
     * - Missing or incomplete database configuration.
     * - Failure to locate the JDBC driver class.
     * - Errors during the database connection establishment.
     * - Unexpected exceptions.
     *
     * If an error occurs, the connection will not be established and the `connection` field remains null.
     */
    public DBContext() {
        Properties properties = new Properties();
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (inputStream == null) {
                LOGGER.severe("Cannot find 'database.properties' file.");
                return;
            }
            properties.load(inputStream);

            String url = properties.getProperty("DB_URL");
            String username = properties.getProperty("DB_USER");
            String password = properties.getProperty("DB_PASSWORD");
            String driver = properties.getProperty("DB_DRIVER");

            if (driver == null || url == null || username == null || password == null) {
                LOGGER.severe("Database configuration is incomplete.");
                return;
            }

            // Nếu cần, có thể bỏ qua Class.forName() nếu driver đã tự đăng ký
            Class.forName(driver);
            connection = DriverManager.getConnection(url, username, password);

        } catch (ClassNotFoundException e) {
            LOGGER.severe("JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            LOGGER.severe("Error establishing connection: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.severe("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Closes the database connection if it is not already closed.
     *
     * This method verifies if the connection instance is not null and ensures
     * that it is not already closed before attempting to close it. Any exceptions
     * encountered while closing the connection are logged using the LOGGER utility.
     *
     * The method is intended to release database resources and should be called
     * when the database connection is no longer needed to prevent resource leaks.
     *
     * If the connection is already closed or null, the method does nothing.
     *
     * Logging:
     * - Logs a severe error message if an SQLException occurs during the close operation.
     */
    protected void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                LOGGER.severe("Error closing database connection: " + e.getMessage());
            }
        }
    }
}