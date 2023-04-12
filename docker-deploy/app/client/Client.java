package client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Client {
    private static Socket socket;
    private final String host;
    private final int PORT_NUM = 12345;
    private int threadSize = 100;

    public Client(String host) throws IOException {
        this.host = host;
        socket = new Socket(this.host,this.PORT_NUM);
    }

    public static void main(String[] args) throws IOException{
        int threadSize = 100;
        ArrayList<Thread> threadPool = new ArrayList<>();
        for(int i = 0; i < threadSize; i++){
            try {
                Client client = new Client("127.0.0.1");
                Request requests = new Request(socket);
                Thread t = new Thread(requests);
                threadPool.add(t);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Timestamp start = Timestamp.from(Instant.now());
        for(Thread t: threadPool){
            t.start();
        }
        for(Thread t: threadPool){
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Timestamp end = Timestamp.from(Instant.now());
        long gap = end.getTime() - start.getTime();
        long seconds = TimeUnit.MILLISECONDS.toSeconds(gap);
        System.out.println(seconds);
    }
}

public class Request implements Runnable{
    public static Socket socket;
    public Request(Socket socket){
        this.socket = socket;
    }

    public static void sendMessage(String msg) throws IOException{
        OutputStream outputStream = socket.getOutputStream();
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.write(msg);
        printWriter.flush();
    }

    public static String receiveMessage() throws IOException{
        InputStream inputStream = socket.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        // skip the first line
        //bufferedReader.readLine();
        StringBuilder req = new StringBuilder(new String());
        String info;
        while ((info = bufferedReader.readLine()) != null) {
            req.append(info+"\n");
        }
        return req.toString();
    }

    @Override
    public void run() {
        try{
            for(int i = 0; i < 100; i++){
                String request = 
                "100000\n"+
                "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" + 
                "<create>\n" + 
                " <account id=\"123456\" balance=\"1000\"/>\n" + 
                " <account id=\"1233456\" balance=\"2000\"/>\n" + 
                " <symbol sym=\"sym\">\n" +
                "  <account id=\"123456\">200</account>\n"+
                "  <account id=\"123456\">200</account>\n"+
                "  <account id=\"100000\">300</account>\n"+
                "<account id=\"100\">100</account>\n"+
                " </symbol>\n"+
                "</create>\n";
                this.sendMessage(request);
                String res = this.receiveMessage();
                System.out.println(res);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}

