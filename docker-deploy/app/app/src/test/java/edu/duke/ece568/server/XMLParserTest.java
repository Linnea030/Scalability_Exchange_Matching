package edu.duke.ece568.server;

import java.sql.SQLException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class XMLParserTest {
        @Test
        public void test_constructor() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);
                // correct
                assertDoesNotThrow(() -> new XMLParser("request", jdbc1));
        }

        public void compare_helper(String request, String exp_res) throws SQLException, Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // correct
                XMLParser parser = new XMLParser(request, jdbc1);
                String response = parser.XMLDisintegrator();
                System.out.println(response);
                assertEquals(exp_res, response);
        }

        public void compare_helperNoDelete(String request, String exp_res) throws SQLException, Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                // Helper.deleteAlltable(jdbc1);

                // correct
                XMLParser parser = new XMLParser(request, jdbc1);
                String response = parser.XMLDisintegrator();
                System.out.println(response);
                assertEquals(exp_res, response);
        }

        @Test
        public void test_wrongFormat() throws Exception {
                String request = "aaaaaa";
                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 1; Content is not allowed in prolog.</error>\n"
                                +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongHeader() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<hhh>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<account id=\"738\" balance=\"2000\"/>" +
                                "</hhh>";
                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Root node is illegal: hhh</error>\n" +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongXML() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<hhh" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<account id=\"738\" balance=\"2000\"/>" +
                                "</hhh>";
                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 43; Element type \"hhh\" must be followed by either attribute specifications, \"&gt;\" or \"/&gt;\".</error>\n"
                                +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_correctCreate1() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate1() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<accoun id=\"123456\" balance=\"1000\"/>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Invalid tag name is read</error>\n" +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_correctCreate2() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">100000</account>" +
                                "</symbol>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <created id=\"123456\" sym=\"sym\"/>\n" +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate2() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<accoun id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">100000</account>" +
                                "</symbol>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Invalid tag name is read</error>\n" +
                                "  <error>org.postgresql.util.PSQLException: Cannot rollback when autoCommit is enabled.</error>\n"
                                +
                                "  <error id=\"123456\" sym=\"sym\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate3() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbo sym=\"sym\">" +
                                "<account id=\"123456\">100000</account>" +
                                "</symbo>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <error>Warning: Invalid tag name is read</error>\n" +
                                "</results>\n";

                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate4() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"12345\">100000</account>" +
                                "</symbol>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <error id=\"12345\" sym=\"sym\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "</results>\n";

                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate5() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">0</account>" +
                                "</symbol>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <error id=\"123456\" sym=\"sym\">java.lang.Exception: Warning: Position amount should not be zero</error>\n"
                                +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate6() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"12345x\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">0</account>" +
                                "</symbol>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error id=\"12345x\">java.lang.NumberFormatException: For input string: \"12345x\"</error>\n"
                                +
                                "  <error id=\"123456\" sym=\"sym\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate7() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">0</account>" +
                                "</symbol>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <error id=\"123456\" sym=\"sym\">java.lang.Exception: Warning: Position amount should not be zero</error>\n"
                                +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongCreate8() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">1000</account>" +
                                "</symbol>" +
                                "<symbol sym=\"hhh\">" +
                                "<account id=\"123456\">1000</account>" +
                                "</symbol>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <created id=\"123456\" sym=\"sym\"/>\n" +
                                "  <created id=\"123456\" sym=\"hhh\"/>\n" +
                                "  <error id=\"123456\">java.lang.Exception: Warning: Duplicated account number created</error>\n"
                                +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_correctCreate3() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">1000</account>" +
                                "</symbol>" +
                                "<symbol sym=\"hhh\">" +
                                "<account id=\"123456\">1000</account>" +
                                "</symbol>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <created id=\"123456\" sym=\"sym\"/>\n" +
                                "  <created id=\"123456\" sym=\"hhh\"/>\n" +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_correctCreate4() throws Exception {
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<create>" +
                                "<account id=\"123456\" balance=\"1000\"/>" +
                                "<symbol sym=\"sym\">" +
                                "<account id=\"123456\">1000</account>" +
                                "</symbol>" +
                                "<symbol sym=\"hhh\">" +
                                "<account id=\"123456\">1000</account>" +
                                "</symbol>" +
                                "<account id=\"12345\" balance=\"1000\"/>" +
                                "</create>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <created id=\"123456\"/>\n" +
                                "  <created id=\"123456\" sym=\"sym\"/>\n" +
                                "  <created id=\"123456\" sym=\"hhh\"/>\n" +
                                "  <created id=\"12345\"/>\n" +
                                "</results>\n";
                this.compare_helper(request, expected);
        }

        @Test
        public void test_wrongTranOrder1() throws Exception {
                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<transactions id=\"123456\">" +
                                "<order sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error amount=\"100\" limit=\"1\" sym=\"sym\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTranCancel1() throws Exception {
                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "<cancel id=\"0\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>java.lang.Exception: Warning: No such related order!</error>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTranQuery1() throws Exception {
                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "<query id=\"0\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <status id=\"0\"/>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTran2() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // new account
                Account a1 = new Account(jdbc1, 123456, 100);
                a1.addAccount();
                Position p1 = new Position(jdbc1, 123456, "sym", 200);
                p1.addSymbol();

                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "<orde sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Invalid tag name is read</error>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTran3() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // new account
                Account a1 = new Account(jdbc1, 123456, 100);
                a1.addAccount();
                Position p1 = new Position(jdbc1, 123456, "sym", 200);
                p1.addSymbol();

                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"12345\">" +
                                "<order amount=\"100\" limit=\"1\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Insufficient attributes of an order request</error>\n" +
                                "  <error amount=\"100\" limit=\"1\" sym=\"\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTran4() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "<query sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Insufficient attributes of a query request</error>\n" +
                                "  <status id=\"\"/>\n" +
                                "  <error>java.lang.NumberFormatException: For input string: \"\"</error>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTran5() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "<cancel sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Insufficient attributes of a cancel request</error>\n" +
                                "  <error>java.lang.NumberFormatException: For input string: \"\"</error>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTran6() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Transaction must have one or more children</error>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTran7() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "<cancel sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "<query sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "<order amount=\"100\" limit=\"1\"/>" +
                                "<orde sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error>Warning: Insufficient attributes of a cancel request</error>\n" +
                                "  <error>java.lang.NumberFormatException: For input string: \"\"</error>\n" +
                                "  <error>Warning: Insufficient attributes of a query request</error>\n" +
                                "  <status id=\"\"/>\n" +
                                "  <error>java.lang.NumberFormatException: For input string: \"\"</error>\n" +
                                "  <error>Warning: Insufficient attributes of an order request</error>\n" +
                                "  <error amount=\"100\" limit=\"1\" sym=\"\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "  <error>Warning: Invalid tag name is read</error>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTranOrder2() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // new account
                Account a1 = new Account(jdbc1, 123456, 1000);
                a1.addAccount();

                Position p1 = new Position(jdbc1, 123456, "sym", 200);
                p1.addSymbol();
                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<transactions id=\"123456\">" +
                                "<order sym=\"sym\" amount=\"100\" limit=\"1\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error amount=\"100\" limit=\"1\" sym=\"sym\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTranCancel2() throws Exception {
                Helper.dropAllTables();
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);

                // new account
                Account a1 = new Account(jdbc1, 123456, 100);
                a1.addAccount();
                Position p1 = new Position(jdbc1, 123456, "sym", 200);
                p1.addSymbol();

                // no order
                String request = "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n"
                                + "<transactions id=\"123456\">\n<order sym=\"sym\" amount=\"100\" limit=\"1\"/>\n</transactions>\n";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <error amount=\"100\" limit=\"1\" sym=\"sym\">java.lang.Exception: Warning: Cannot find the specified account</error>\n"
                                +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }

        @Test
        public void test_wrongTranQuery2() throws Exception {
                PostgreSQLJDBC jdbc1 = Helper.connectJDBC("localhost", "5432", "postgres", "postgres", "postgres");
                Helper.deleteAlltable(jdbc1);
                // new account
                Account a1 = new Account(jdbc1, 123456, 100);
                a1.addAccount();
                Position p1 = new Position(jdbc1, 123456, "sym", 200);
                p1.addSymbol();
                // no order
                String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<transactions id=\"123456\">" +
                                "<query id=\"0\"/>" +
                                "</transactions>";

                String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                                "<results>\n" +
                                "  <status id=\"0\"/>\n" +
                                "</results>\n";
                this.compare_helperNoDelete(request, expected);
        }
}
