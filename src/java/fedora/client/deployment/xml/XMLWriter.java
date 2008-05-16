/* The contents of this file are subject to the license and copyright terms
 * detailed in the license directory at the root of the source tree (also 
 * available online at http://www.fedora.info/license/).
 */

package fedora.client.deployment.xml;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fedora.utilities.XmlTransformUtility;

/**
 * @author Sandy Payette
 */
public class XMLWriter {

    private final Element rootElement;

    public XMLWriter(DOMResult result) {
        rootElement = (Element) result.getNode().getFirstChild();
    }

    public XMLWriter(Document document) {
        rootElement = document.getDocumentElement();
    }

    public XMLWriter(Element root) {
        rootElement = root;
    }

    public String getXMLAsString() throws TransformerException,
            TransformerConfigurationException, ParserConfigurationException {
        Writer w = new StringWriter();
        PrintWriter out = new PrintWriter(w);

        TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
        Transformer transformer = tfactory.newTransformer();
        Properties transProps = new Properties();
        transProps.put("method", "xml");
        transProps.put("indent", "yes");
        transProps.put("omit-xml-declaration", "yes");
        transformer.setOutputProperties(transProps);
        transformer
                .transform(new DOMSource(rootElement), new StreamResult(out));
        out.close();
        return w.toString();
    }

    public void writeXMLToFile(File file) throws TransformerException,
            TransformerConfigurationException, ParserConfigurationException,
            IOException {
        PrintWriter out =
                new PrintWriter(new BufferedWriter(new FileWriter(file)));
        TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
        Transformer transformer = tfactory.newTransformer();
        Properties transProps = new Properties();
        transProps.put("method", "xml");
        transProps.put("indent", "yes");
        transProps.put("omit-xml-declaration", "no");
        transformer.setOutputProperties(transProps);
        transformer
                .transform(new DOMSource(rootElement), new StreamResult(out));
        out.close();
        return;
    }

    public InputStream writeXMLToStream() throws TransformerException,
            TransformerConfigurationException, ParserConfigurationException,
            IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TransformerFactory tfactory = XmlTransformUtility.getTransformerFactory();
        Transformer transformer = tfactory.newTransformer();
        Properties transProps = new Properties();
        transProps.put("method", "xml");
        transProps.put("indent", "yes");
        transProps.put("omit-xml-declaration", "no");
        transformer.setOutputProperties(transProps);
        transformer
                .transform(new DOMSource(rootElement), new StreamResult(out));
        InputStream in = new ByteArrayInputStream(out.toByteArray());
        out.close();
        return in;
    }

    /**
     * Serializes the specified node, recursively, to a Writer and returns it as
     * a String too.
     */
    public String serializeRoot(Writer out) {
        return serializeNode(rootElement, out);
    }

    private String serializeNode(Node node, Writer out) {
        StringBuffer string = new StringBuffer();

        try {
            if (node == null) {
                return null;
            }

            int type = node.getNodeType();
            switch (type) {
                case Node.DOCUMENT_NODE:
                    //out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    //string.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n");
                    serializeNode(((Document) node).getDocumentElement(), out);
                    break;

                case Node.ELEMENT_NODE:
                    string.append("<");
                    string.append(node.getNodeName());

                    out.write("<");
                    out.write(node.getNodeName());

                    // do attributes
                    NamedNodeMap attrs = node.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        string.append(" ");
                        string.append(attrs.item(i).getNodeName());
                        string.append("=\"");
                        string.append(attrs.item(i).getNodeValue());
                        string.append("\"");

                        out.write(" ");
                        out.write(attrs.item(i).getNodeName());
                        out.write("=\"");
                        out.write(attrs.item(i).getNodeValue());
                        out.write("\"");
                    }

                    // close up the current element
                    string.append(">");
                    out.write(">");

                    // recursive call to process this node's children
                    NodeList children = node.getChildNodes();
                    if (children != null) {
                        int len = children.getLength();
                        for (int i = 0; i < len; i++) {
                            serializeNode(children.item(i), out);
                        }
                    }
                    break;

                case Node.TEXT_NODE:
                    string.append(node.getNodeValue());
                    out.write(node.getNodeValue());
                    break;
            }

            if (type == Node.ELEMENT_NODE) {
                string.append("</");
                string.append(node.getNodeName());
                string.append(">");
                out.write("</");
                out.write(node.getNodeName());
                out.write(">");
            }
            out.flush();
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return enc(string.toString());
        //return(string.toString());
    }

    /**
     * Returns an XML-appropriate encoding of the given String.
     * 
     * @param in
     *        The String to encode.
     * @return A new, encoded String.
     */
    private static String enc(String in) {
        StringBuffer out = new StringBuffer();
        enc(in, out);
        //System.out.println(out.toString());
        return out.toString();
    }

    /**
     * Appends an XML-appropriate encoding of the given String to the given
     * StringBuffer.
     * 
     * @param in
     *        The String to encode.
     * @param buf
     *        The StringBuffer to write to.
     */
    private static void enc(String in, StringBuffer out) {
        for (int i = 0; i < in.length(); i++) {
            enc(in.charAt(i), out);
        }
    }

    /**
     * Appends an XML-appropriate encoding of the given character to the given
     * StringBuffer.
     * 
     * @param in
     *        The character.
     * @param out
     *        The StringBuffer to write to.
     */
    private static void enc(char in, StringBuffer out) {
        if (in == '&') {
            out.append("&amp;");
        } else if (in == '<') {
            out.append("&lt;");
        } else if (in == '>') {
            out.append("&gt;");
        } else if (in == '\"') {
            out.append("&quot;");
        } else if (in == '\'') {
            out.append("&apos;");
        } else {
            out.append(in);
        }
    }
}