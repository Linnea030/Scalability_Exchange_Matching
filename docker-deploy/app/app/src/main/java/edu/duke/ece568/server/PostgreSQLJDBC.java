package edu.duke.ece568.server;

import java.sql.*;

//Database Connection and Creation
public class PostgreSQLJDBC {
   private Connection conn = null;

   public PostgreSQLJDBC(String serverUrl, String serverPort, String dbName, String userName, String userPassword)
         throws SQLException, ClassNotFoundException {
      this.getConnectionToDB(serverUrl, serverPort, dbName, userName, userPassword);
      this.dropTables();
      this.createAccountTable();
      this.createPositionTable();
      this.createOrderTable();
      this.createExcutedOrderTable();
   }

   private void getConnectionToDB(String serverUrl, String serverPort, String dbName, String userName,
         String userPassword)
         throws SQLException, ClassNotFoundException {
      System.out.println("Notice: Getting connection to the database: " + dbName);
      Class.forName("org.postgresql.Driver");
      this.conn = DriverManager.getConnection("jdbc:postgresql://" + serverUrl + ":" + serverPort + "/" + dbName,
            userName, userPassword);
      System.out.println("Notice: Opened database: " + dbName + " successfully");
   }

   public synchronized void updateDB(String query) throws SQLException {
      Statement stat = this.conn.createStatement();
      stat.executeUpdate(query);
      stat.close();
   }

   public synchronized int updateDBAndReturnID(String query) throws SQLException {
      Statement stat = this.conn.createStatement();
      stat.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
      ResultSet res = stat.getGeneratedKeys();
      int pid = -1;
      if (res.next()) {
         pid = res.getInt(1);
      }
      return pid;
   }

   public synchronized ResultSet queryDB(String query) throws SQLException {
      Statement stat = this.conn.createStatement();
      ResultSet res = stat.executeQuery(query);
      return res;
   }

   private void dropTables() throws SQLException {
      String query = "DROP TABLE IF EXISTS ACCOUNT CASCADE;";
      query += "DROP TABLE IF EXISTS POSITION CASCADE;";
      query += "DROP TABLE IF EXISTS ORDER_INFO CASCADE;";
      query += "DROP TABLE IF EXISTS EXECUTEDORDER CASCADE;";
      this.updateDB(query);
   }

   private void createAccountTable() throws SQLException {
      String query = "CREATE TABLE IF NOT EXISTS ACCOUNT"
            + "(ACCOUNT_NUMBER INT PRIMARY KEY,"
            + "BALANCE FLOAT NOT NULL CHECK (BALANCE >= 0));";
      this.updateDB(query);
   }

   private void createPositionTable() throws SQLException {
      String query = "CREATE TABLE IF NOT EXISTS POSITION"
            + "(POSITION_ID SERIAL PRIMARY KEY,"
            + "SYMBOL VARCHAR (255) NOT NULL,"
            + "ACCOUNT_NUMBER INT NOT NULL,"
            + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0),"
            + "UNIQUE (ACCOUNT_NUMBER, SYMBOL),"
            + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
      this.updateDB(query);
   }

   private void createOrderTable() throws SQLException {
      String query = "CREATE TABLE IF NOT EXISTS ORDER_INFO("
            + "ORDER_ID SERIAL PRIMARY KEY,"
            + "SYMBOL VARCHAR (255) NOT NULL,"
            + "ACCOUNT_NUMBER INT NOT NULL,"
            + "ORDER_STATUS VARCHAR (255) NOT NULL,"
            + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0), "
            + "TYPE VARCHAR (255) NOT NULL,"
            + "LIMIT_PRICE FLOAT NOT NULL CHECK (LIMIT_PRICE > 0),"
            + "CREATED_TIME BIGINT NOT NULL,"
            + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
      this.updateDB(query);
   }

   private void createExcutedOrderTable() throws SQLException {
      String query = "CREATE TABLE IF NOT EXISTS EXECUTEDORDER"
            + "(EXECUTEDORDER_ID SERIAL PRIMARY KEY,"
            + "ORDER_ID INT NOT NULL,"
            + "SYMBOL VARCHAR (255) NOT NULL,"
            + "TYPE VARCHAR (255) NOT NULL,"
            + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0), "
            + "PRICE FLOAT NOT NULL CHECK (PRICE > 0),"
            + "CREATED_TIME BIGINT NOT NULL);";
      this.updateDB(query);
   }

   public Connection getDBConnection() {
      return this.conn;
   }

   public void closeDBConnection() throws SQLException {
      this.conn.close();
   }

}
