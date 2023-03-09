import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class XmiModelElementTest {

    private static String testFileName = "xml-simple.xml";
    private static final Logger logger = Logger.getLogger(XmiModelElementTest.class.getName());
    private static XPath xpath = null;

    private static Document document = null;

    private static InputStream loadFile() throws IOException {
            var file = XmiModelElementTest.class
                    .getClassLoader().getResourceAsStream(testFileName);
            return file;
    }

    @BeforeAll
    public static void init() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.parse(loadFile());

        xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return switch (prefix) {
                    case "uml" -> "http://schema.omg.org/spec/UML/2.1";
                    case "xmi" -> "http://schema.omg.org/spec/XMI/2.1";
                    default -> null;
                };
            }

            public Iterator<String> getPrefixes(String val) {
                return null;
            }

            public String getPrefix(String uri) {
                return null;
            }
        });
    }

    @Test
    public void shouldValidateFirstElement() {
        Element element = XmiModelElementTest.document.getDocumentElement();
        element.normalize();
        NodeList nodeList = element.getElementsByTagName("packagedElement");
        assertNotEquals(0, nodeList.getLength());

        Node first = nodeList.item(0);

        assertEquals(Node.ELEMENT_NODE, first.getNodeType());
        assertEquals("packagedElement", first.getNodeName());
    }

    @Test
    public void shouldOutputClassNames() throws XPathExpressionException {
        //        String expression = "/xmi:XMI/xmi:Documentation/uml:Model/packagedElement[@name='Model']";

//        XPathExpression expr = xpath.compile("//xmi:XMI/uml:Model/packagedElement[@xmi:type='uml:Class']");
        XPathExpression expr = xpath.compile("//xmi:XMI/uml:Model//packagedElement[@xmi:type='uml:Class']");
        Object result = expr.evaluate(document, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        assertNotEquals(0, nodes.getLength());
        logger.log(Level.INFO, "Total Class Names Found: " + nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node currentItem = nodes.item(i);
            logger.log(Level.FINE,"found node -> " + currentItem.getLocalName() + " (namespace: " + currentItem.getNamespaceURI() + ")");
            logger.log(Level.INFO, currentItem.getAttributes().getNamedItem("name").getNodeValue());
        }
    }

    @Test
    public void shouldOutputPropertyNames() throws XPathExpressionException {
        XPathExpression expr = xpath.compile("//xmi:XMI/uml:Model//packagedElement/ownedAttribute[@xmi:type='uml:Property']");
        Object result = expr.evaluate(document, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        assertNotEquals(0, nodes.getLength());
        logger.log(Level.INFO, "Total Property Names Found: " + nodes.getLength());
        for (int i = 0; i < nodes.getLength(); i++) {
            Node currentItem = nodes.item(i);
            logger.log(Level.FINE,"found node -> " + currentItem.getLocalName() + " (namespace: " + currentItem.getNamespaceURI() + ")");
            logger.log(Level.INFO, currentItem.getAttributes().getNamedItem("name").getNodeValue());
        }
    }
}
