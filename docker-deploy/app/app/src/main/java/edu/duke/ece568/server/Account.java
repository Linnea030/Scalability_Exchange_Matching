package edu.duke.ece568.server;

import java.sql.*;

public class Account {
    private PostgreSQLJDBC jdbc;
    private int accountID;
    private double balance;

    public Account(PostgreSQLJDBC jdbc, int accountID, double balance) {
        this.jdbc = jdbc;
        this.accountID = accountID;
        this.balance = balance;
    }

    public void addAccount() throws Exception {
        String check = "SELECT * FROM ACCOUNT WHERE ACCOUNT_NUMBER=" + this.accountID + ";";
        ResultSet set = this.jdbc.queryDB(check);
        if (set.next()) {
            throw new Exception("Warning: Duplicated account number created");
        }
        String query = "INSERT INTO ACCOUNT(ACCOUNT_NUMBER, BALANCE) VALUES (" + this.accountID + ", " + this.balance
                + ");";
        this.jdbc.updateDB(query);
    }

    public int buyOrSellStock(String symbol, double price) throws Exception {
        double amount = this.balance;
        if (amount == 0 || price <= 0) {
            throw new Exception("Warning: Order amount should not be zero and limit price should be possitive");
        }
        // get account balance
        String query = "SELECT * FROM ACCOUNT WHERE ACCOUNT_NUMBER=" + accountID + ";";
        ResultSet res = this.jdbc.queryDB(query);
        if (!res.next()) {
            throw new Exception("Warning: Cannot find the specified account");
        }
        double accountBalance = Double.parseDouble(res.getString("BALANCE"));
        this.balance = accountBalance;
        // buy stocks
        if (amount > 0) {
            if (accountBalance < amount * price) {
                throw new Exception("Warning: Not enough balance to buy this amount of stocks");
            }
            // update balance
            this.updateBalance(-(amount * price));
            Order buyOrder = new Order(this.jdbc, symbol, this.accountID, amount, price);
            buyOrder.matchSellOrder();
            return buyOrder.getPkey();
        }
        // sell stocks
        else {
            String query2 = "SELECT AMOUNT FROM POSITION WHERE ACCOUNT_NUMBER=" + this.accountID + " AND SYMBOL=\'"
                    + symbol + "\' AND AMOUNT >= " + amount + ";";
            ResultSet res2 = this.jdbc.queryDB(query2);
            if (!res2.next()) {
                throw new Exception("Warning: Not enough shares to sell");
            }
            // update position
            Position sp = new Position(this.jdbc, this.accountID, symbol, amount);
            sp.addSymbol();
            Order sellOrder = new Order(this.jdbc, symbol, this.accountID, amount, price);
            sellOrder.matchBuyOrder();
            return sellOrder.getPkey();
        }
    }

    public void updateBalance(double difference) throws SQLException {
        double updatedBalance = this.balance + difference;
        String query = "UPDATE ACCOUNT SET BALANCE=" + updatedBalance + " WHERE ACCOUNT_NUMBER=" + this.accountID
                + ";";
        this.jdbc.updateDB(query);
    }

}
