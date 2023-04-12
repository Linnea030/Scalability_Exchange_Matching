package edu.duke.ece568.server;

import java.sql.SQLException;
import java.io.*;
import org.junit.jupiter.api.*;

public class MessageTest {
    public Client newClient() throws IOException {
        return new Client("127.0.0.1");
    }

    @Test
    public void test_send() throws SQLException, Exception, IOException {
        // Server s1 = new Server();
        // Socket clientSocket = s1.getServerSocket().accept();
        // Client c1 = newClient();

        // Message m1 = new Message(clientSocket);

        // String msg = "Hi";
        // c1.sendMessage(msg);
        // assertEquals(msg, m1.messageReader());
        // test with new normal account
    }
}
