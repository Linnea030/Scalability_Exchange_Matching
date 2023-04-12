package edu.duke.ece568.server;

import java.sql.*;

public class Position {
    private PostgreSQLJDBC jdbc;
    private int accountID;
    private String symbol;
    private double amount;

    public Position(PostgreSQLJDBC jdbc, int accountID, String symbol, double amount) throws Exception {
        this.jdbc = jdbc;
        this.accountID = accountID;
        this.symbol = symbol;
        this.amount = amount;
        if (amount == 0) {
            throw new Exception("Warning: Position amount should not be zero");
        }
    }

    public void addSymbol() throws Exception {
        String query = "SELECT * FROM POSITION WHERE ACCOUNT_NUMBER=" + this.accountID + " AND SYMBOL=\'" + symbol
                + "\';";
        ResultSet res = this.jdbc.queryDB(query);
        if (!res.next()) {
            System.out.println("Notice: Create a new position -> (" + symbol + ":" + amount + ") in ID: " + accountID);
            String query2 = "INSERT INTO POSITION(SYMBOL, ACCOUNT_NUMBER, AMOUNT) VALUES (\'" + this.symbol + "\', "
                    + this.accountID + ", " + this.amount + ");";
            this.jdbc.updateDB(query2);
            return;
        }
        double originalAmount = res.getDouble("AMOUNT");
        System.out.println(
                "Notice: Update the position -> (" + this.symbol + ":" + originalAmount + ") in ID: " + this.accountID);
        double updatedAmount = originalAmount + this.amount;
        if (updatedAmount == 0) {
            String query3 = "DELETE FROM POSITION WHERE ACCOUNT_NUMBER=" + this.accountID + " AND SYMBOL=\'"
                    + this.symbol + "\';";
            this.jdbc.updateDB(query3);
        } else if (updatedAmount > 0) {
            String query4 = "UPDATE POSITION SET AMOUNT=" + updatedAmount + " WHERE ACCOUNT_NUMBER=" + this.accountID
                    + " AND SYMBOL=\'" + this.symbol + "\';";
            this.jdbc.updateDB(query4);
        } else {
            throw new Exception("Warning: Position amount should not be negative");
        }
    }

    public double getTotalAmount() throws SQLException {
        String query = "SELECT * FROM POSITION WHERE ACCOUNT_NUMBER=" + this.accountID + " AND SYMBOL=\'" + symbol
                + "\';";
        ResultSet res = this.jdbc.queryDB(query);
        if (!res.next()) {
            System.out.println("Notice: Create a new position -> (" + symbol + ":" + amount + ") in ID: " + accountID);
            String query2 = "INSERT INTO POSITION(SYMBOL, ACCOUNT_NUMBER, AMOUNT) VALUES (\'" + this.symbol + "\', "
                    + this.accountID + ", " + this.amount + ");";
            this.jdbc.updateDB(query2);
            return this.amount;
        }
        double originalAmount = res.getDouble("AMOUNT");
        return originalAmount;
    }

}
