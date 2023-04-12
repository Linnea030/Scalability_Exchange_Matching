package edu.duke.ece568.server;

import java.sql.SQLException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AccountTest {
    @Test
    public void test_Account() throws SQLException, Exception {
        PostgreSQLJDBC jdbc = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        Helper.deleteAlltable(jdbc);
        // test with new normal account
        Account a1 = new Account(jdbc, 0, 50);
        assertDoesNotThrow(() -> a1.addAccount());

        // test addAccount function
        Account a2 = new Account(jdbc, 1, 200);
        assertThrows(Exception.class, () -> a2.buyOrSellStock("hhh", 20));

        // test buyOrSellStock function
        assertDoesNotThrow(() -> a2.addAccount());
        assertThrows(Exception.class, () -> a2.buyOrSellStock("hhh", -10));
        assertThrows(Exception.class, () -> a2.buyOrSellStock("hhh", 0));

        // test multiple same account
        Account a3 = new Account(jdbc, 1, 60);
        assertThrows(Exception.class, () -> a3.addAccount());
    }
}
