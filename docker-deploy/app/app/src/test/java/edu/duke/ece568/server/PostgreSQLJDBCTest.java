package edu.duke.ece568.server;

import java.sql.SQLException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class PostgreSQLJDBCTest {
        @Test
        public void test_JDBCConnect() throws ClassNotFoundException, SQLException, Exception {
                // correct
                assertDoesNotThrow(() -> Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres"));
                // wrong password
                assertThrows(SQLException.class,
                                () -> Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "111"));
                // wrong dbname
                assertThrows(SQLException.class,
                                () -> Helper.connectJDBC("localhost", "5433", "postgre1", "postgres", "postgres"));
                // wrong portNum
                assertThrows(SQLException.class,
                                () -> Helper.connectJDBC("localhost", "123", "postgres", "postgres", "postgres"));
                // wrong localhost
                assertThrows(SQLException.class,
                                () -> Helper.connectJDBC("local0", "5432", "postgres", "postgres", "postgres"));
                // wrong username
                assertThrows(SQLException.class,
                                () -> Helper.connectJDBC("localhost", "5432", "postgres", "hhhhhh", "postgres"));
                // all wrong
                assertThrows(SQLException.class, () -> Helper.connectJDBC("h", "666", "hhh", "hhhhhh", "123456"));
        }

        @Test
        public void test_createTable() throws SQLException, Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");

                // test create account table again
                String query1 = "CREATE TABLE IF NOT EXISTS ACCOUNT"
                                + "(ACCOUNT_NUMBER INT PRIMARY KEY,"
                                + "BALANCE FLOAT NOT NULL CHECK (BALANCE >= 0));";
                assertDoesNotThrow(() -> jdbc1.updateDB(query1));

                // test create position table again
                String query2 = "CREATE TABLE IF NOT EXISTS POSITION"
                                + "(POSITION_ID SERIAL PRIMARY KEY,"
                                + "SYMBOL VARCHAR (255) NOT NULL,"
                                + "ACCOUNT_NUMBER INT NOT NULL,"
                                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0),"
                                + "UNIQUE (ACCOUNT_NUMBER, SYMBOL),"
                                + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
                assertDoesNotThrow(() -> jdbc1.updateDB(query2));

                // test create order_info table again
                String query3 = "CREATE TABLE IF NOT EXISTS ORDER_INFO("
                                + "ORDER_ID SERIAL PRIMARY KEY,"
                                + "SYMBOL VARCHAR (255) NOT NULL,"
                                + "ACCOUNT_NUMBER INT NOT NULL,"
                                + "ORDER_STATUS VARCHAR (255) NOT NULL,"
                                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0), "
                                + "TYPE VARCHAR (255) NOT NULL,"
                                + "LIMIT_PRICE FLOAT NOT NULL CHECK (LIMIT_PRICE > 0),"
                                + "CREATED_TIME TIMESTAMP NOT NULL,"
                                + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
                assertDoesNotThrow(() -> jdbc1.updateDB(query3));

                // test create executedorder table
                String query4 = "CREATE TABLE IF NOT EXISTS EXECUTEDORDER"
                                + "(EXECUTEDORDER_ID SERIAL PRIMARY KEY,"
                                + "ORDER_ID INT NOT NULL,"
                                + "SYMBOL VARCHAR (255) NOT NULL,"
                                + "TYPE VARCHAR (255) NOT NULL,"
                                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0), "
                                + "PRICE FLOAT NOT NULL CHECK (PRICE > 0),"
                                + "CREATED_TIME TIMESTAMP NOT NULL);";
                assertDoesNotThrow(() -> jdbc1.updateDB(query4));
        }

        @Test
        public void test_dropTable() throws SQLException, Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.dropAllTables();

                // test create order_info table again
                String query3 = "CREATE TABLE IF NOT EXISTS ORDER_INFO("
                                + "ORDER_ID SERIAL PRIMARY KEY,"
                                + "SYMBOL VARCHAR (255) NOT NULL,"
                                + "ACCOUNT_NUMBER INT NOT NULL,"
                                + "ORDER_STATUS VARCHAR (255) NOT NULL,"
                                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0), "
                                + "TYPE VARCHAR (255) NOT NULL,"
                                + "LIMIT_PRICE FLOAT NOT NULL CHECK (LIMIT_PRICE > 0),"
                                + "CREATED_TIME TIMESTAMP NOT NULL,"
                                + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
                assertThrows(SQLException.class, () -> jdbc1.updateDB(query3));

                // test create position table again
                String query2 = "CREATE TABLE IF NOT EXISTS POSITION"
                                + "(POSITION_ID SERIAL PRIMARY KEY,"
                                + "SYMBOL VARCHAR (255) NOT NULL,"
                                + "ACCOUNT_NUMBER INT NOT NULL,"
                                + "AMOUNT FLOAT NOT NULL CHECK (AMOUNT > 0),"
                                + "UNIQUE (ACCOUNT_NUMBER, SYMBOL),"
                                + "CONSTRAINT FK_ACCOUNT_NUMBER FOREIGN KEY (ACCOUNT_NUMBER) REFERENCES ACCOUNT(ACCOUNT_NUMBER) ON UPDATE CASCADE ON DELETE CASCADE);";
                assertThrows(SQLException.class, () -> jdbc1.updateDB(query2));
        }

}