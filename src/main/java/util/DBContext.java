package util;

import java.sql.Connection;
import java.util.Properties;
import java.util.logging.Logger;

public class DBContext {
    protected Connection connection;

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
                Logger.getLogger("DBContext").info("Database connection established successfully.");
            } else {
                Logger.getLogger("DBContext").warning("Failed to establish database connection.");
            }
        } catch (Exception e) {
            Logger.getLogger("DBContext").severe("Database connection error: " + e.getMessage());
        }
    }

    public boolean checkConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            Logger.getLogger("DBContext").severe("Error checking connection: " + e.getMessage());
            return false;
        }
    }
}
