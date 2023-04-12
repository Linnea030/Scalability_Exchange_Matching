package edu.duke.ece568.server;

import java.sql.SQLException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class PositionTest {
    @Test
    public void test_constructor() throws Exception {
        PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        // amount != 0
        assertDoesNotThrow(() -> new Position(jdbc1, 0, "sym", 100));
        // amount == 0
        assertThrows(Exception.class, () -> new Position(jdbc1, 0, "sym", 0));
    }

    @Test
    public void test_addSymbol() throws Exception, SQLException {
        PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
        Position p1 = new Position(jdbc1, 0, "sym", 100);

        // test no exist account
        assertThrows(SQLException.class, () -> p1.addSymbol());

        // test exist account
        Account a1 = new Account(jdbc1, 0, 50);
        assertDoesNotThrow(() -> a1.addAccount());
        assertDoesNotThrow(() -> p1.addSymbol());

        // test add + position amount
        Position p2 = new Position(jdbc1, 0, "sym", 100);
        assertDoesNotThrow(() -> p2.addSymbol());
        assertEquals(200, p2.getTotalAmount());

        // test add - position amount
        Position p3 = new Position(jdbc1, 0, "sym", -50);
        assertDoesNotThrow(() -> p3.addSymbol());
        assertEquals(150, p2.getTotalAmount());

        // test add to negative amount
        Position p4 = new Position(jdbc1, 0, "sym", -500);
        assertThrows(Exception.class, () -> p4.addSymbol());

        // test add amount == 0 and delete
        Position p5 = new Position(jdbc1, 0, "sym", -150);
        assertDoesNotThrow(() -> p5.addSymbol());
        assertThrows(SQLException.class, () -> p5.getTotalAmount());
    }
}
