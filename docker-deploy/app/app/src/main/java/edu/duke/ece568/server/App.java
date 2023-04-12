package edu.duke.ece568.server;

public class App {
    public static void main(String[] args) {
        try {
            Server server = new Server();
            server.runServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
