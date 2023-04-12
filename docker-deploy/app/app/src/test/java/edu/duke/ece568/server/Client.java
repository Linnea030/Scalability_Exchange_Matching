package edu.duke.ece568.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private static Socket socket;
    private final String host;
    private final int PORT_NUM = 12345;

    public Client(String host) throws IOException {
        this.host = host;
        socket = new Socket(this.host, this.PORT_NUM);
    }

    public Socket getSocket() {
        return this.socket;
    }

    public static void main(String[] args) throws IOException {
        String request = "127\n" +
                "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" +
                "<create>\n" +
                "<account id=\"123456\" balance=\"1000\"/>\n" +
                "<account id=\"100000\" balance=\"2000\"/>\n" +
                "<symbol sym=\"AAA\">\n" +
                "<account id=\"123456\">100</account>\n" +
                "<account id=\"123456\">200</account>\n" +
                "<account id=\"100000\">100</account>\n" +
                "<account id=\"100\">100</account>\n" +
                "</symbol>\n" +
                "</create>\n";
        new Client("127.0.0.1");
        System.out.println(request);
        sendMessage(request);
        String response = receiveMessage();
        System.out.println(response);
    }

    public static void sendMessage(String msg) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.write(msg);
        printWriter.flush();
    }

    public static String receiveMessage() throws IOException {
        InputStream in = socket.getInputStream();
        var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        return reader.readLine();
    }
}
