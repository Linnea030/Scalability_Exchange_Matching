package edu.duke.ece568.server;

import java.sql.*;
//import java.time.Instant;
import java.util.LinkedList;

public class Order {
    private PostgreSQLJDBC jdbc;
    private int pkey;
    private String symbol;
    private int accountID;
    private String status;
    private double amount;
    private String type;
    private double limitPrice;
    // private Timestamp time;
    private long time;

    public Order(PostgreSQLJDBC jdbc, String symbol, int accountID, double amount, double limitPrice)
            throws Exception {
        this.jdbc = jdbc;
        this.symbol = symbol;
        this.accountID = accountID;
        this.status = "OPENED";
        if (amount > 0) {
            this.amount = amount;
            this.type = "Buy";
        } else if (amount < 0) {
            this.amount = -amount;
            this.type = "Sell";
        } else {
            throw new Exception("Warning: Order amount should not be zero");
        }
        this.limitPrice = limitPrice;
        this.time = System.currentTimeMillis() / 1000;
        this.pkey = addOrder();
    }

    public Order(PostgreSQLJDBC jdbc, int pkey) {
        this.jdbc = jdbc;
        this.pkey = pkey;
    }

    public Order(PostgreSQLJDBC jdbc, int pkey, String symbol, int accountID, String status, double amount, String type,
            double limitPrice, long time) {
        this.jdbc = jdbc;
        this.pkey = pkey;
        this.symbol = symbol;
        this.accountID = accountID;
        this.status = status;
        this.amount = amount;
        this.type = type;
        this.limitPrice = limitPrice;
        this.time = time;
    }

    private int addOrder() throws SQLException {
        String query = "INSERT INTO ORDER_INFO (SYMBOL, ACCOUNT_NUMBER, ORDER_STATUS, AMOUNT, TYPE, LIMIT_PRICE, CREATED_TIME) VALUES(\'"
                + this.symbol + "\', " + this.accountID + ", \'" + this.status + "\', " + this.amount + ", \'"
                + this.type + "\', "
                + this.limitPrice
                + ", \'" + this.time + "\'" + ");";
        int pkey = this.jdbc.updateDBAndReturnID(query);
        return pkey;
    }

    private Account retrieveAccount(int accountNum) throws Exception {
        String query = "SELECT * FROM ACCOUNT WHERE ACCOUNT_NUMBER=" + accountNum + ";";
        ResultSet res = this.jdbc.queryDB(query);
        if (!res.next()) {
            throw new Exception("Warning: Cannot find the account");
        }
        double accountBalance = Double.parseDouble(res.getString("BALANCE"));
        Account acc = new Account(this.jdbc, accountNum, accountBalance);
        return acc;
    }

    private Order retrieveOrder(ResultSet set) throws NumberFormatException, SQLException {
        int orderID = set.getInt("ORDER_ID");
        String sym = set.getString("SYMBOL");
        int accountNum = set.getInt("ACCOUNT_NUMBER");
        String status = set.getString("ORDER_STATUS");
        double amount = set.getDouble("AMOUNT");
        String type = set.getString("TYPE");
        double limitPrice = set.getDouble("LIMIT_PRICE");
        long createdTime = set.getLong("CREATED_TIME");
        Order myorder = new Order(this.jdbc, orderID, sym, accountNum, status, amount, type, limitPrice, createdTime);
        return myorder;
    }

    public synchronized void matchSellOrder() throws Exception {
        String query = "SELECT * FROM ORDER_INFO WHERE SYMBOL = \'" + this.symbol + "\' AND ACCOUNT_NUMBER <> "
                + this.accountID
                + " AND ORDER_STATUS = \'OPENED\' AND AMOUNT > 0 AND TYPE = \'Sell\' AND LIMIT_PRICE <= "
                + this.limitPrice + " ORDER BY LIMIT_PRICE ASC, CREATED_TIME ASC;";
        ResultSet res = this.jdbc.queryDB(query);
        while (res.next() && this.amount > 0) {
            Order sellerOrder = this.retrieveOrder(res);
            Account sellerAccount = this.retrieveAccount(sellerOrder.getAccountID());
            Account buyerAccount = this.retrieveAccount(this.accountID);
            double sellerAmount = sellerOrder.getAmount();
            double price = sellerOrder.limitPrice;
            if (this.amount >= sellerAmount) {
                // seller
                sellerAccount.updateBalance((sellerAmount * price));
                sellerOrder.addExecutedOrder(sellerAmount, price);
                sellerOrder.executeOpenOrder();
                // buyer
                buyerAccount.updateBalance((sellerAmount * Math.abs(this.limitPrice - price))); // refund
                this.addExecutedOrder(sellerAmount, price);
                this.amount -= sellerAmount;
                this.updateOrderAmount();
                Position bp = new Position(this.jdbc, this.getAccountID(), this.getSymbol(),
                        sellerAmount);
                bp.addSymbol();
            } else {
                // seller
                sellerAccount.updateBalance((this.amount * price));
                sellerOrder.addExecutedOrder(this.amount, price);
                sellerOrder.setAmount(sellerOrder.getAmount() - this.amount);
                sellerOrder.updateOrderAmount();
                // buyer
                buyerAccount.updateBalance(this.amount * Math.abs(this.limitPrice - price)); // refund
                this.addExecutedOrder(this.amount, price);
                Position bp = new Position(this.jdbc, this.getAccountID(), this.getSymbol(),
                        this.amount);
                bp.addSymbol();
                this.amount = 0;
            }
        }
        if (this.amount == 0) {
            this.executeOpenOrder();
        }
    }

    public synchronized void matchBuyOrder() throws Exception {
        String query = "SELECT * FROM ORDER_INFO WHERE SYMBOL = \'" + this.symbol + "\' AND ACCOUNT_NUMBER <> "
                + this.accountID
                + " AND ORDER_STATUS = \'OPENED\' AND AMOUNT > 0 AND TYPE = \'Buy\' AND LIMIT_PRICE >= "
                + this.limitPrice + " ORDER BY LIMIT_PRICE DESC, CREATED_TIME ASC;";
        ResultSet res = this.jdbc.queryDB(query);
        while (res.next() && this.amount > 0) {
            Order buyerOrder = this.retrieveOrder(res);
            Account sellerAccount = this.retrieveAccount(this.accountID);
            double buyerAmount = buyerOrder.getAmount();
            double price = buyerOrder.limitPrice;
            if (this.amount >= buyerAmount) {
                // buyer
                buyerOrder.addExecutedOrder(buyerAmount, price);
                buyerOrder.executeOpenOrder();
                Position bp = new Position(this.jdbc, buyerOrder.getAccountID(), buyerOrder.getSymbol(),
                        buyerAmount);
                bp.addSymbol();
                // seller
                sellerAccount.updateBalance(buyerAmount * price);
                this.addExecutedOrder(buyerAmount, price);
                this.amount -= buyerAmount;
                this.updateOrderAmount();
            } else {
                // buyer
                buyerOrder.addExecutedOrder(this.amount, price);
                buyerOrder.setAmount(buyerOrder.getAmount() - this.amount);
                buyerOrder.updateOrderAmount();
                Position bp = new Position(this.jdbc, buyerOrder.getAccountID(), buyerOrder.getSymbol(),
                        this.amount);
                bp.addSymbol();
                // seller
                sellerAccount.updateBalance((this.amount * price));
                this.addExecutedOrder(this.amount, price);
                this.amount = 0;
            }
        }
        if (this.amount == 0) {
            this.executeOpenOrder();
        }
    }

    private void addExecutedOrder(double orderAmount, double orderPrice) throws SQLException {
        String query = "INSERT INTO EXECUTEDORDER (ORDER_ID, SYMBOL, TYPE, AMOUNT, PRICE, CREATED_TIME) VALUES("
                + this.pkey
                + ", \'" + this.symbol + "\', \'" + this.type + "\', " + orderAmount + ", " + orderPrice + ", \'"
                + this.time + "\');";
        this.jdbc.updateDB(query);
    }

    private void executeOpenOrder() throws SQLException {
        String query = "DELETE FROM ORDER_INFO WHERE ORDER_ID=" + this.pkey + ";";
        this.jdbc.updateDB(query);
    }

    private void updateOrderAmount() throws SQLException {
        String query = "UPDATE ORDER_INFO SET AMOUNT=" + this.amount + " WHERE ORDER_ID=" + this.pkey + ";";
        this.jdbc.updateDB(query);
    }

    public static LinkedList<Order> listOrder(
            PostgreSQLJDBC jdbc, String DBName, int tranID, String orderStatus) throws SQLException {
        String query = "SELECT * FROM " + DBName + " WHERE ORDER_ID=" + tranID;
        if (DBName.equals("ORDER_INFO")) {
            query += (" AND ORDER_STATUS=\'" + orderStatus + "\';");
        } else {
            query += ";";
        }
        ResultSet res = jdbc.queryDB(query);
        LinkedList<Order> orders = new LinkedList<>();
        while (res.next()) {
            int orderID = res.getInt("ORDER_ID");
            String sym = res.getString("SYMBOL");
            double amount = res.getDouble("AMOUNT");
            String type = res.getString("TYPE");
            long time = res.getLong("CREATED_TIME");
            Order myOrder = null;
            if (DBName.equals("ORDER_INFO")) {
                int accountNum = res.getInt("ACCOUNT_NUMBER");
                String st = res.getString("ORDER_STATUS");
                double price = res.getDouble("LIMIT_PRICE");
                myOrder = new Order(jdbc, orderID, sym, accountNum, st, amount, type, price, time);
            } else {
                int accountNum = 0;
                String st = "";
                double price = res.getDouble("PRICE");
                myOrder = new Order(jdbc, orderID, sym, accountNum, st, amount, type, price, time);
            }
            orders.add(myOrder);
        }
        return orders;
    }

    // cancel order
    public synchronized void cancelTheOrder() throws Exception {
        // find related order which is open by ID
        this.findOpenOrder();
        if (this.pkey > 0) {
            String query = "";
            query += "UPDATE ORDER_INFO ";
            query += "SET ORDER_STATUS = \'CANCELLED\'";
            // query += ", CREATED_TIME = \'" + (System.currentTimeMillis() / 1000) + "\'";
            query += " WHERE ORDER_ID = " + this.pkey;
            query += " AND ORDER_STATUS = \'OPENED\';";
            // execute the query in DB
            this.jdbc.updateDB(query);
            // add stock back to seller
            if (this.type.equals("Sell")) {
                String que = "SELECT * FROM POSITION WHERE ACCOUNT_NUMBER = " + this.accountID;
                que += (" AND SYMBOL = \'" + this.symbol + "\';");
                ResultSet res = this.jdbc.queryDB(que);
                if (res.next()) {
                    double amountOri = res.getDouble("AMOUNT");
                    double totalAmount = amountOri + this.amount;
                    que = "";
                    que += ("UPDATE POSITION ");
                    que += ("SET AMOUNT=" + totalAmount);
                    que += (" WHERE ACCOUNT_NUMBER=" + this.accountID);
                    que += (" AND SYMBOL=\'" + this.symbol + "\';");
                    this.jdbc.updateDB(que);
                } else {
                    throw new Exception("Warning: No such account of seller");
                }

            }
            // refund money to buyer
            else if (this.type.equals("Buy")) { // if sale order, add back stock
                String que = "SELECT * FROM ACCOUNT WHERE ACCOUNT_NUMBER = " + this.accountID + ";";
                ResultSet res = this.jdbc.queryDB(que);
                if (res.next()) {
                    double balance = res.getDouble("BALANCE");
                    double totalBalance = balance + this.limitPrice * this.amount;
                    que = "";
                    que += ("UPDATE ACCOUNT ");
                    que += ("SET BALANCE=" + totalBalance);
                    que += (" WHERE ACCOUNT_NUMBER=" + this.accountID + ";");
                    this.jdbc.updateDB(que);
                } else {
                    throw new Exception("Warning: No such account of buyer");
                }
            }
        } else {
            throw new Exception("Warning: This order can not be canceled!");
        }
    }

    // Find all related orders by ID
    private ResultSet findOrderByID(int mode) throws SQLException, Exception {
        String query = "";
        if (mode == 0) {
            // mode = 0 for open and cancelled order
            query = "SELECT * FROM ORDER_INFO WHERE ORDER_ID = " + this.pkey + ";";
        } else if (mode == 1) {
            // mode = 1 for executed order
            query = "SELECT * FROM EXECUTEDORDER WHERE ORDER_ID = " + this.pkey + ";";
        }
        ResultSet res = this.jdbc.queryDB(query);
        return res;
    }

    public ResultSet findOrderByStatus(String status, int mode) throws SQLException, Exception {
        String query = "";
        if (mode == 0) {
            // mode = 0 for open and cancelled order
            query = "SELECT * FROM ORDER_INFO WHERE ORDER_ID = " + this.pkey;
            query += " AND ORDER_STATUS = \'" + status + "\';";
        } else if (mode == 1) {
            // mode = 1 for executed order
            query = "SELECT * FROM EXECUTEDORDER WHERE ORDER_ID = " + this.pkey + ";";
        }
        ResultSet res = this.jdbc.queryDB(query);
        return res;
    }

    private void findOpenOrder() throws SQLException, Exception {
        ResultSet res = this.findOrderByID(0);
        Boolean findOpen = false;
        while (res.next()) {
            if (res.getString("ORDER_STATUS").equals("OPENED")) {
                findOpen = true;
                break;
            }
        }
        if (findOpen == false) {
            throw new Exception("Warning: No such related order!");
        } else {
            this.pkey = res.getInt("ORDER_ID");
            this.symbol = res.getString("SYMBOL");
            this.accountID = res.getInt("ACCOUNT_NUMBER");
            this.status = res.getString("ORDER_STATUS");
            this.amount = res.getDouble("AMOUNT");
            this.type = res.getString("TYPE");
            this.limitPrice = res.getDouble("LIMIT_PRICE");
            this.time = res.getLong("CREATED_TIME");
        }
    }

    public void updateInfo(ResultSet res, int mode) throws Exception {
        if (mode == 0) {
            this.pkey = res.getInt("ORDER_ID");
            this.symbol = res.getString("SYMBOL");
            this.accountID = res.getInt("ACCOUNT_NUMBER");
            this.status = res.getString("ORDER_STATUS");
            this.amount = res.getDouble("AMOUNT");
            this.type = res.getString("TYPE");
            this.limitPrice = res.getDouble("LIMIT_PRICE");
            this.time = res.getLong("CREATED_TIME");
        } else {
            this.pkey = res.getInt("ORDER_ID");
            this.symbol = res.getString("SYMBOL");
            this.type = res.getString("TYPE");
            this.amount = res.getDouble("AMOUNT");
            this.limitPrice = res.getDouble("PRICE");
            this.time = res.getLong("CREATED_TIME");
        }
    }

    public int getPkey() {
        return this.pkey;
    }

    public int getAccountID() {
        return this.accountID;
    }

    public double getAmount() {
        return this.amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSymbol() {
        return this.symbol;
    }

    public long getTime() {
        return this.time;
    }

    public double getPrice() {
        return this.limitPrice;
    }

    public String getStatus() {
        return this.status;
    }

}
