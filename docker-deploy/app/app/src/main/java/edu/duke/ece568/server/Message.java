package edu.duke.ece568.server;

import java.net.*;
import java.io.*;

public class Message {
    private Socket clientSocket = null;

    public Message(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public String messageReader() throws IOException {
        InputStream inputStream = this.clientSocket.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        // skip the first line
        int len = Integer.parseInt(bufferedReader.readLine());
        StringBuilder req = new StringBuilder(new String());
        // String info;
        while (true) {
            int c = bufferedReader.read();
            req.append((char) c);
            len--;
            if (String.valueOf(req).contains("</create>")
                    || String.valueOf(req).contains("</transactions>") || len == 0)
                break;
        }
        System.out.println(req.toString());
        return req.toString();
    }

    public void messageSender(String res) throws IOException {
        OutputStream outputStream = this.clientSocket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.write(res + "\n");
        printWriter.flush();
    }

}
