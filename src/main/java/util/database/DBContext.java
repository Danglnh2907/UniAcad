package util.database;

import java.sql.Connection;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The DBContext class is responsible for managing the database connection
 * to enable interaction with a relational database. It uses configuration
 * properties from an external file to establish the connection with the
 * database driver and URL.
 *
 * The class provides functionality to initialize a database connection
 * during instantiation and verifies the connection's status.
 *
 * The database connection is managed as a protected member to allow
 * subclasses to access it for performing database operations.
 *
 * In case of any issues during the database connection process, appropriate
 * logs are generated to provide feedback on the operation's status.
 */
public class DBContext {
    protected static Connection connection;
    private static final String NAME_CLASS = "DBContext";

    public static Connection getConnection() {
        return connection;
    }

    /**
     * Constructor for the DBContext class.
     * Initializes a connection to the database using configuration properties
     * retrieved from an external file. The properties file must contain the
     * database driver, URL, username, and password.
     *
     * The constructor performs the following operations:
     * 1. Loads the database configuration properties from a file located at "/database.properties".
     * 2. Loads the database driver class specified in the configuration properties.
     * 3. Establishes a connection to the database using the URL, username, and password.
     * 4. Checks if the connection is successfully established using the checkConnection() method.
     * 5. Logs the success or failure of the database connection attempt.
     *
     * If an error occurs during the process, it logs the error message.
     */
    public DBContext() {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getResourceAsStream("/database.properties"));

            Class.forName(properties.getProperty("DB_DRIVER"));
            connection = java.sql.DriverManager.getConnection(
                    properties.getProperty("DB_URL"),
                    properties.getProperty("DB_USERNAME"),
                    properties.getProperty("DB_PASSWORD")
            );
            if (checkConnection())
            {
                Logger.getLogger(NAME_CLASS).info("Database connection established successfully.");
            } else {
                Logger.getLogger(NAME_CLASS).warning("Failed to establish database connection.");
            }
        } catch (Exception e) {
            Logger.getLogger(NAME_CLASS).severe("Database connection error: " + e.getMessage());
        }
    }

    /**
     * Checks the status of the database connection.
     *
     * This method verifies if the database connection is not null and has not been closed.
     * It catches any exceptions that might occur during the verification process and logs
     * an error message if an issue is encountered.
     *
     * @return true if the connection is not null and is open, otherwise false.
     */
    public boolean checkConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            Logger.getLogger(NAME_CLASS).severe("Error checking connection: " + e.getMessage());
            return false;
        }
    }
}
