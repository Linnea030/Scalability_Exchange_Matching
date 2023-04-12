package edu.duke.ece568.server;

import java.sql.SQLException;

public class Helper {
    public static PostgreSQLJDBC connectJDBC(String localhost, String portNum, String dbName,
            String userName, String userPassword) throws SQLException, Exception {
        return new PostgreSQLJDBC(localhost, portNum, dbName, userName, userPassword);
    }

    public static void deleteAlltable(PostgreSQLJDBC jdbc) throws SQLException, Exception {
        String que = "DELETE FROM ACCOUNT; " +
                "DELETE FROM POSITION; " +
                "DELETE FROM ORDER_INFO; " +
                "DELETE FROM EXECUTEDORDER; ";
        jdbc.updateDB(que);
    }

    public static void dropAllTables() throws SQLException, Exception {
        PostgreSQLJDBC jdbc = connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        String que = "DROP TABLE IF EXISTS ACCOUNT, POSITION, ORDER_INFO, EXECUTEDORDER;";
        jdbc.updateDB(que);
    }

}
