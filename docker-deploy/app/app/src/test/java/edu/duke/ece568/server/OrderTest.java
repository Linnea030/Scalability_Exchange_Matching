package edu.duke.ece568.server;

import java.sql.*;
import java.sql.SQLException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {
    @Test
    public void test_constructor() throws Exception {
        PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        Helper.deleteAlltable(jdbc1);
        // correct
        assertDoesNotThrow(() -> new Order(jdbc1, 0));
        assertDoesNotThrow(() -> new Order(jdbc1, 0, "sym", 0, "OPEN", 200.6, "Buy", 1, 8000));

        // No account exist
        assertThrows(SQLException.class, () -> new Order(jdbc1, "sym", 0, 100, 100));

        // account exist but no sym
        Account a1 = new Account(jdbc1, 0, 500);
        assertDoesNotThrow(() -> a1.addAccount());
        assertDoesNotThrow(() -> new Order(jdbc1, "sym", 0, 100, 100));
        assertDoesNotThrow(() -> new Order(jdbc1, "sym", 0, -100, 100));

        // account exist with sym
        Position p1 = new Position(jdbc1, 0, "sym", 100);
        assertDoesNotThrow(() -> p1.addSymbol());
        assertDoesNotThrow(() -> new Order(jdbc1, "sym", 0, 100, 1));

        // amount == 0
        assertThrows(Exception.class, () -> new Order(jdbc1, "sym", 0, 0, 100));
    }

    @Test
    public void test_matchOrder() throws Exception {
        PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        Helper.deleteAlltable(jdbc1);

        // create new accounts
        Account a1 = new Account(jdbc1, 0, 500);
        Account a2 = new Account(jdbc1, 1, 500);
        Account a3 = new Account(jdbc1, 2, 500);
        assertDoesNotThrow(() -> a1.addAccount());
        assertDoesNotThrow(() -> a2.addAccount());
        assertDoesNotThrow(() -> a3.addAccount());

        // buy order amount > sale order amount
        Position p1 = new Position(jdbc1, 1, "sym", 100);
        Position p2 = new Position(jdbc1, 2, "sym", 100);
        assertDoesNotThrow(() -> p1.addSymbol());
        assertDoesNotThrow(() -> p2.addSymbol());
        Order buy1 = new Order(jdbc1, "sym", 0, 100, 1);
        assertDoesNotThrow(() -> buy1.matchBuyOrder());
        Order sale1 = new Order(jdbc1, "sym", 1, -20, 0.9);
        assertDoesNotThrow(() -> sale1.matchSellOrder());
        Order sale2 = new Order(jdbc1, "sym", 2, -60, 0.8);
        assertDoesNotThrow(() -> sale2.matchSellOrder());

        // buy order amount == sale order amount
        Order buy2 = new Order(jdbc1, "sym", 1, 10, 1);
        assertDoesNotThrow(() -> buy2.matchBuyOrder());
        Order sale3 = new Order(jdbc1, "sym", 2, -10, 0.9);
        assertDoesNotThrow(() -> sale3.matchSellOrder());

    }

    @Test
    public void test_cancelOrder() throws Exception {
        PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        Helper.deleteAlltable(jdbc1);

        // create new accounts
        Account a1 = new Account(jdbc1, 0, 500);
        Account a2 = new Account(jdbc1, 1, 500);
        Account a3 = new Account(jdbc1, 2, 500);
        assertDoesNotThrow(() -> a1.addAccount());
        assertDoesNotThrow(() -> a2.addAccount());
        assertDoesNotThrow(() -> a3.addAccount());

        // buy order amount > sale order amount
        Position p1 = new Position(jdbc1, 1, "sym", 100);
        Position p2 = new Position(jdbc1, 2, "sym", 100);
        assertDoesNotThrow(() -> p1.addSymbol());
        assertDoesNotThrow(() -> p2.addSymbol());
        Order buy1 = new Order(jdbc1, "sym", 0, 100, 1);
        assertDoesNotThrow(() -> buy1.matchBuyOrder());
        Order sale1 = new Order(jdbc1, "sym", 1, -20, 0.9);
        assertDoesNotThrow(() -> sale1.matchSellOrder());
        Order sale2 = new Order(jdbc1, "sym", 2, -60, 0.8);
        assertDoesNotThrow(() -> sale2.matchSellOrder());

        // buy order amount == sale order amount
        Order buy2 = new Order(jdbc1, "sym", 1, 10, 1);
        assertDoesNotThrow(() -> buy2.matchBuyOrder());
        Order sale3 = new Order(jdbc1, "sym", 2, -10, 0.9);
        assertDoesNotThrow(() -> sale3.matchSellOrder());

        // buy order amount < sale order amount
        Order buy3 = new Order(jdbc1, "sym", 1, 1, 1);
        assertDoesNotThrow(() -> buy3.matchBuyOrder());
        Order sale4 = new Order(jdbc1, "sym", 2, -2, 0.9);
        assertDoesNotThrow(() -> sale4.matchSellOrder());
    }

    @Test
    public void test_cancel() throws Exception, SQLException {
        PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        Helper.deleteAlltable(jdbc1);

        // create accounts
        Account a1 = new Account(jdbc1, 0, 500);
        assertDoesNotThrow(() -> a1.addAccount());

        // success: cancel a buy order
        Order bo1 = new Order(jdbc1, "sym", 0, 100, 1);
        assertEquals("OPENED", bo1.getStatus());
        assertDoesNotThrow(() -> bo1.cancelTheOrder());

        ResultSet res_open1 = bo1.findOrderByStatus("OPENED", 0);
        ResultSet res_cancel1 = bo1.findOrderByStatus("CANCELLED", 0);
        ResultSet res_exe1 = bo1.findOrderByStatus("EXECUTED", 1);
        res_open1.next();
        res_cancel1.next();
        assertThrows(Exception.class, () -> bo1.updateInfo(res_open1, 0));
        assertDoesNotThrow(() -> bo1.updateInfo(res_cancel1, 0));
        assertEquals("CANCELLED", bo1.getStatus());
        assertThrows(Exception.class, () -> bo1.cancelTheOrder());

        // cancel a sale order
        Position p1 = new Position(jdbc1, 0, "sym", 100);
        assertDoesNotThrow(() -> p1.addSymbol());
        Order so1 = new Order(jdbc1, "sym", 0, -100, 1);
        assertEquals("OPENED", so1.getStatus());
        assertDoesNotThrow(() -> so1.cancelTheOrder());

        ResultSet res_open = so1.findOrderByStatus("OPENED", 0);
        ResultSet res_cancel = so1.findOrderByStatus("CANCELLED", 0);
        ResultSet res_exe = so1.findOrderByStatus("EXECUTED", 1);
        res_open.next();
        res_cancel.next();
        assertThrows(Exception.class, () -> so1.updateInfo(res_open, 0));
        assertDoesNotThrow(() -> so1.updateInfo(res_cancel, 0));
        assertEquals("CANCELLED", so1.getStatus());
        assertThrows(Exception.class, () -> so1.cancelTheOrder());
    }
}
