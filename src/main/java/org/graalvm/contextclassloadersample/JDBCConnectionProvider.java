package org.graalvm.contextclassloadersample;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;


public class JDBCConnectionProvider {
    public static Enumeration<Driver> getDrivers() {
        return DriverManager.getDrivers();
    }

    public static Connection getConnection(String url) throws SQLException {
        return DriverManager.getConnection(url);
    }
}
