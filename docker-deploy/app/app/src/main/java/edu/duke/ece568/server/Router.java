package edu.duke.ece568.server;

import java.net.*;
//import java.io.IOException;

public class Router extends Thread {
    private final PostgreSQLJDBC jdbc;
    private final Socket clientSocket;
    // private final ServerSocket serverSocket;

    public Router(PostgreSQLJDBC jdbc, Socket clientSocket, ServerSocket serverSocket)
            throws Exception {
        this.jdbc = jdbc;
        this.clientSocket = clientSocket;
        // this.serverSocket = serverSocket;
        this.routeRequest();
    }

    private void routeRequest() throws Exception {
        // read client's request content
        Message clientMessage = new Message(this.clientSocket);
        String clientRequest = clientMessage.messageReader();
        System.out.println(clientRequest);
        // xml parser
        XMLParser xp = new XMLParser(clientRequest, this.jdbc);
        String response = xp.XMLDisintegrator();
        // System.out.print(response);
        // send response
        clientMessage.messageSender(response);
        // close sockets and streams
        this.clientSocket.shutdownInput();
        this.clientSocket.shutdownOutput();
        this.clientSocket.close();
    }

}
