package edu.duke.ece568.server;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

public class XMLParser {
    private PostgreSQLJDBC jdbc;
    private String request;
    private Document response;
    private Element xmlBase;

    public XMLParser(String request, PostgreSQLJDBC jdbc) {
        this.request = request;
        this.jdbc = jdbc;
        this.response = null;
        this.xmlBase = null;
    }

    protected String XMLDisintegrator() throws Exception {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            this.response = builder.newDocument();
            Document doc = builder.parse(new InputSource(new StringReader(this.request)));
            doc.getDocumentElement().normalize();
            // this.response = builder.newDocument();

            // get root element name
            Node rootNode = doc.getChildNodes().item(0);
            String rootName = rootNode.getNodeName();

            // create response xml
            this.xmlBase = this.response.createElement("results");
            this.response.appendChild(this.xmlBase);

            // <create>
            if (rootName.equals("create")) {
                this.createHandler(rootNode);
                return this.sendResponse();
            }
            // <transaction>
            if (rootName.equals("transactions")) {
                this.transactionHandler(rootNode);
                return this.sendResponse();
            }

            Element xmlNode = this.response.createElement("error");
            String err = "Warning: Root node is illegal: " + rootName;
            xmlNode.appendChild(this.response.createTextNode(err));
            this.xmlBase.appendChild(xmlNode);
            return this.sendResponse();

        } catch (Exception e) {
            this.xmlBase = this.response.createElement("results");
            this.response.appendChild(this.xmlBase);
            Element xmlNode = this.response.createElement("error");
            xmlNode.appendChild(this.response.createTextNode(e.toString()));
            this.xmlBase.appendChild(xmlNode);
            return this.sendResponse();
        }
    }

    private void createHandler(Node rootNode) {
        NodeList childList = rootNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element childNode = (Element) childList.item(i);
                String childName = childNode.getNodeName();
                if (childName.equals("account")) {
                    String accountNum = childNode.getAttribute("id");
                    String balance = childNode.getAttribute("balance");
                    this.createAccount(accountNum, balance);
                } else if (childName.equals("symbol")) {
                    String sym = childNode.getAttribute("sym");
                    NodeList gchildList = childNode.getElementsByTagName("account");
                    for (int j = 0; j < gchildList.getLength(); j++) {
                        if (gchildList.item(j).getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element gchild = (Element) gchildList.item(j);
                        String accountNumber = gchild.getAttribute("id");
                        String amount = gchild.getTextContent();
                        this.createSymbol(sym, accountNumber, amount);
                    }
                } else {
                    this.xmlError("Warning: Invalid tag name is read");
                }
            }
        }
    }

    private void createAccount(String accountNum, String balance) {
        try {
            this.jdbc.getDBConnection().setAutoCommit(false);
            Account myAccount = new Account(jdbc, Integer.parseInt(accountNum), Double.parseDouble(balance));
            myAccount.addAccount();
            this.jdbc.getDBConnection().commit();

            // result response
            Element xmlNode = this.response.createElement("created");
            xmlNode.setAttribute("id", accountNum);
            this.xmlBase.appendChild(xmlNode);

        } catch (Exception e) {
            try {
                this.jdbc.getDBConnection().rollback();
            } catch (SQLException se) {
                this.xmlError(se.toString());
            }
            Element xmlNode = this.response.createElement("error");
            xmlNode.setAttribute("id", accountNum);
            xmlNode.appendChild(this.response.createTextNode(e.toString()));
            this.xmlBase.appendChild(xmlNode);
        }
    }

    private void createSymbol(String sym, String accountNum, String amount) {
        try {
            this.checkIDExist(accountNum);
            this.jdbc.getDBConnection().setAutoCommit(false);
            Position myPosition = new Position(jdbc, Integer.parseInt(accountNum), sym, Double.parseDouble(amount));
            myPosition.addSymbol();
            this.jdbc.getDBConnection().commit();

            // result response
            Element xmlNode = this.response.createElement("created");
            xmlNode.setAttribute("sym", sym);
            xmlNode.setAttribute("id", accountNum);
            this.xmlBase.appendChild(xmlNode);

        } catch (Exception e) {
            try {
                this.jdbc.getDBConnection().rollback();
            } catch (SQLException se) {
                this.xmlError(se.toString());
            }
            Element xmlNode = this.response.createElement("error");
            xmlNode.setAttribute("sym", sym);
            xmlNode.setAttribute("id", accountNum);
            xmlNode.appendChild(this.response.createTextNode(e.toString()));
            this.xmlBase.appendChild(xmlNode);
        }
    }

    private void checkIDExist(String accountNum) throws Exception {
        String query = "SELECT * FROM ACCOUNT WHERE ACCOUNT_NUMBER=" + accountNum + ";";
        ResultSet res = this.jdbc.queryDB(query);
        if (!res.next()) {
            throw new Exception("Warning: Cannot find the specified account");
        }
    }

    private void xmlError(String errorMsg) {
        Element xmlNode = this.response.createElement("error");
        xmlNode.appendChild(this.response.createTextNode(errorMsg));
        this.xmlBase.appendChild(xmlNode);
    }

    private void transactionHandler(Node rootNode) throws Exception {
        String accountID = ((Element) rootNode).getAttribute("id");
        if (accountID.equals("")) {
            this.xmlError("Warning: Transaction must have an account id");
        }
        int count = 0;
        NodeList childList = rootNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            if (childList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element em = (Element) childList.item(i);
                if (em.getNodeName().equals("order")) {
                    String sym = em.getAttribute("sym");
                    String amount = em.getAttribute("amount");
                    String limit = em.getAttribute("limit");
                    if (sym.equals("") || amount.equals("") || limit.equals("")) {
                        this.xmlError("Warning: Insufficient attributes of an order request");
                    }
                    this.openOrder(accountID, sym, amount, limit);
                } else if (em.getNodeName().equals("query")) {
                    String id = em.getAttribute("id");
                    if (id.equals("")) {
                        this.xmlError("Warning: Insufficient attributes of a query request");
                    }
                    this.queryOrder(id);
                } else if (em.getNodeName().equals("cancel")) {
                    String id = em.getAttribute("id");
                    if (id.equals("")) {
                        this.xmlError("Warning: Insufficient attributes of a cancel request");
                    }
                    this.cancelOrder(id);
                } else {
                    this.xmlError("Warning: Invalid tag name is read");
                }
                count++;
            }
        }
        if (count == 0) {
            this.xmlError("Warning: Transaction must have one or more children");
        }
    }

    private void openOrder(String accountID, String sym, String amount, String limit) {
        try {
            this.jdbc.getDBConnection().setAutoCommit(false);
            Account myaccount = new Account(this.jdbc, Integer.parseInt(accountID), Double.parseDouble(amount));
            int id = myaccount.buyOrSellStock(sym, Double.parseDouble(limit));
            this.jdbc.getDBConnection().commit();

            // result response
            Element xmlNode = this.response.createElement("opened");
            xmlNode.setAttribute("sym", sym);
            xmlNode.setAttribute("amount", amount);
            xmlNode.setAttribute("limit", limit);
            xmlNode.setAttribute("id", String.valueOf(id));
            this.xmlBase.appendChild(xmlNode);
        } catch (Exception e) {
            try {
                this.jdbc.getDBConnection().rollback();
            } catch (SQLException se) {
                this.xmlError(se.toString());
            }
            Element xmlNode = this.response.createElement("error");
            xmlNode.setAttribute("sym", sym);
            xmlNode.setAttribute("amount", amount);
            xmlNode.setAttribute("limit", limit);
            xmlNode.appendChild(this.response.createTextNode(e.toString()));
            this.xmlBase.appendChild(xmlNode);
        }
    }

    private void queryOrder(String tranID) {
        try {
            Element xmlNode = this.response.createElement("status");
            xmlNode.setAttribute("id", tranID);
            this.xmlBase.appendChild(xmlNode);
            this.getAllOpenedOrders(Integer.parseInt(tranID), xmlNode);
            this.getAllCancelledOrders(Integer.parseInt(tranID), xmlNode);
            this.getAllExcutedOrders(Integer.parseInt(tranID), xmlNode);
        } catch (Exception e) {
            this.xmlError(e.toString());
        }
    }

    private void getAllOpenedOrders(int tranID, Element xmlChild) throws SQLException {
        LinkedList<Order> openOrders = Order.listOrder(this.jdbc, "ORDER_INFO", tranID, "OPENED");
        for (Order order : openOrders) {
            Element xmlNode = this.response.createElement("open");
            xmlNode.setAttribute("shares", String.valueOf(order.getAmount()));
            xmlChild.appendChild(xmlNode);
        }
    }

    private void getAllCancelledOrders(int tranID, Element xmlChild) throws SQLException {
        LinkedList<Order> cancelledOrders = Order.listOrder(this.jdbc, "ORDER_INFO", tranID, "CANCELLED");
        for (Order order : cancelledOrders) {
            Element xmlNode = this.response.createElement("canceled");
            xmlNode.setAttribute("shares", String.valueOf(order.getAmount()));
            xmlNode.setAttribute("time", String.valueOf(order.getTime()));
            xmlChild.appendChild(xmlNode);
        }
    }

    private void getAllExcutedOrders(int tranID, Element xmlChild) throws SQLException {
        LinkedList<Order> excutedOrders = Order.listOrder(this.jdbc, "EXECUTEDORDER", tranID, "");
        for (Order order : excutedOrders) {
            Element xmlNode = this.response.createElement("executed");
            xmlNode.setAttribute("shares", String.valueOf(order.getAmount()));
            xmlNode.setAttribute("price", String.valueOf(order.getPrice()));
            xmlNode.setAttribute("time", String.valueOf(order.getTime()));
            xmlChild.appendChild(xmlNode);
        }
    }

    private void cancelOrder(String id) {
        try {
            // cancel order by function cancelOrder() in Order class
            this.jdbc.getDBConnection().setAutoCommit(false);
            int key = Integer.parseInt(id);
            Order cancelledOrder = new Order(this.jdbc, key);
            cancelledOrder.cancelTheOrder();
            this.jdbc.getDBConnection().commit();

            // get all related orders by order id
            ResultSet cancelledRes = cancelledOrder.findOrderByStatus("CANCELLED", 0);
            ResultSet executedRes = cancelledOrder.findOrderByStatus("EXECUTED", 1);

            // generate response
            Element xmlNode = this.response.createElement("canceled");
            xmlNode.setAttribute("id", id);
            this.xmlBase.appendChild(xmlNode);

            // generate response about cancelled order
            if (cancelledRes.next()) {
                // update information of order by resultset
                cancelledOrder.updateInfo(cancelledRes, 0);
                Element xmlNode1 = this.response.createElement("canceled");
                xmlNode1.setAttribute("shares", Double.toString(cancelledOrder.getAmount()));
                xmlNode1.setAttribute("time", String.valueOf(cancelledOrder.getTime()));
                xmlNode.appendChild(xmlNode1);
            }

            // generate response about executed order
            while (executedRes.next()) {
                // update information of order by resultset one by one
                cancelledOrder.updateInfo(executedRes, 1);
                Element xmlNode1 = this.response.createElement("executed");
                xmlNode1.setAttribute("shares", Double.toString(cancelledOrder.getAmount()));
                xmlNode1.setAttribute("price", Double.toString(cancelledOrder.getPrice()));
                xmlNode1.setAttribute("time", String.valueOf(cancelledOrder.getTime()));
                xmlNode.appendChild(xmlNode1);
            }
        } catch (Exception e) {
            try {
                this.jdbc.getDBConnection().rollback();
            } catch (SQLException se) {
                this.xmlError(se.toString());
            }
            Element xmlNode = this.response.createElement("error");
            xmlNode.appendChild(this.response.createTextNode(e.toString()));
            this.xmlBase.appendChild(xmlNode);
        }
    }

    private String sendResponse() throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(this.response), new StreamResult(outputStream));
        String output = outputStream.toString().replaceAll("\n|\r", "");
        String resp = this.format(output);
        // System.out.println(resp);
        return resp;
    }

    private String format(String xmlString) throws Exception {
        try {
            InputSource source = new InputSource(new StringReader(xmlString));
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            Writer output = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            return output.toString();
        } catch (Exception e) {
            throw new Exception("Warning: Error occurs when formatting xml:\n" + xmlString, e);
        }
    }

}