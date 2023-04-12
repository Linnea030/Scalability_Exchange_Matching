package client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private static Socket socket;
    private final String host;
    private final int PORT_NUM = 12345;

    public Client(String host) throws IOException {
        this.host = host;
        socket = new Socket(this.host,this.PORT_NUM);
    }

    public static void main(String[] args) throws IOException{
        
        String request = 
        "100000\n"+
        "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" + 
        "<create>\n" + 
         " <account id=\"123456\" balance=\"1000\"/>\n" + 
         " <account id=\"1233456\" balance=\"2000\"/>\n" + 
         " <symbol sym=\"sym\">\n" +
         "  <account id=\"123456\">200</account>\n"+
         //"  <account id=\"123456\">200</account>\n"+
         //"  <account id=\"100000\">300</account>\n"+
         //"<account id=\"100\">100</account>\n"+
         " </symbol>\n"+
        "</create>\n";
        request ="11111\n"+ "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" +
                "<transactions id=\"123456\">\n" +
                "<order sym=\"sym\" amount=\"100\" limit=\"1\"/>\n" +
                "</transactions>\n";
        //request =  "127\n"+"<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" + "<transactions id=\"123456\">\n<order sym=\"sym\" amount=\"100\" limit=\"1\"/>\n</transactions>\n";
        //request =  "127\n"+"<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" + "<transactions id=\"100000\">\n<order sym=\"AAA\" amount=\"-5\" limit=\"8.9\"/>\n</transactions>\n";
        //request = "127\n"+"<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" + "<transactions id=\"123456\">\n<query id=\"1\"/>\n</transactions>\n";
        //request = "127\n"+"<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n" + "<transactions id=\"100000\">\n<cancel id=\"1\"/>\n</transactions>\n";
        File fileinput = new File("query2.xml");
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fileinput);
        DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);

        new Client("127.0.0.1");
        //System.out.println(request);
        sendMessage(writer.toString());
        String response = receiveMessage();
        System.out.println(response);
        //String request2 = "<transactions id=\"ACCOUNT_ID\"><order sym=\"SYM\" amount=\"AMT\" limit=\"LMT\"/><query id=\"TRANS_ID\"><cancel id=\"TRANS_ID\"></transactions>";
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
}
