package edu.duke.ece568.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private ServerSocket serverSocket = null;
    public final int portNum = 12345;
    public final int threadPoolSize = 100;
    public final ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);
    private int threadID = 0;

    public Server() throws IOException {
        this.serverSocket = new ServerSocket(portNum);
    }

    private Socket acceptClientSockets() throws IOException {
        Socket clientSocket = this.serverSocket.accept();
        return clientSocket;
    }

    // start method
    public void runServer() throws Exception {
        PostgreSQLJDBC jdbc = new PostgreSQLJDBC("db", "5432", "postgres", "postgres", "postgres");
        while (true) {
            Socket clientSocket = this.acceptClientSockets();
            this.threadID++;
            System.out.println("Notice: A new request ID(" + this.threadID + ") received");
            threadPool.execute(new Router(jdbc, clientSocket, this.serverSocket));
        }
    }
}
