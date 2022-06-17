package dev.anthonyashco.sqlserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionUtil {
    public static Connection connect(String connectionString) {
        System.out.println(connectionString);
        try {
            return DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Connection connect(String connectionString, Properties credentials) {
        System.out.println(connectionString);
        try {
            return DriverManager.getConnection(connectionString, credentials);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Connection connect(String connectionString, String user, String pass) {
        System.out.println(connectionString);
        try {
            return DriverManager.getConnection(connectionString, user, pass);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}