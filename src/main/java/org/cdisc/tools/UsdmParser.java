package org.cdisc.tools;

import org.w3c.dom.Document;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UsdmParser {

    private static final String UML_NAMESPACE_URI = "http://schema.omg.org/spec/UML/2.1";
    private static final String XMI_NAMESPACE_URI = "http://schema.omg.org/spec/XMI/2.1";

    private static final Logger logger = Logger.getLogger(UsdmParser.class.getName());

    private Document document = null;

    public UsdmParser(InputStream file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        this.document = builder.parse(file);
    }

    public void loadFromUsdmXmi(Map<String, ModelClass> elements) throws XPathExpressionException {
        XPath xPath = setUpPath();
        XPathExpression expr = xPath.compile("//xmi:XMI/uml:Model//packagedElement[@xmi:type='uml:Class']");
        Object result = expr.evaluate(document, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() > 0) {
            logger.log(Level.FINE, "Total Classes Found: " + nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Node currentItem = nodes.item(i);
                Map<String, ModelClassProperty> properties = new HashMap<>();
                String className = currentItem.getAttributes().getNamedItem("name").getNodeValue();
                elements.put(className, new ModelClass(className, properties, null));
                ////xmi:XMI/uml:Model//packagedElement[@name='Activity']/ownedAttribute[@xmi:type='uml:Property']
                String propertyExprStr = String.format("//xmi:XMI/uml:Model//packagedElement[@name='%1$s']/" +
                        "ownedAttribute[@xmi:type='uml:Property']", className);
                logger.log(Level.FINE, propertyExprStr);
                XPathExpression propertyExpr = xPath.compile(propertyExprStr);
                Object propsResult = propertyExpr.evaluate(this.document, XPathConstants.NODESET);
                NodeList propsNodes = (NodeList) propsResult;
                logger.log(Level.FINE, "Total Properties Found: " + propsNodes.getLength());
                for (int j = 0; j < propsNodes.getLength(); j++) {
                    Node currentProp = propsNodes.item(j);
                    String propName = currentProp.getAttributes().getNamedItem("name").getNodeValue();
                    properties.put(propName, new ModelClassProperty(propName, null, null));
                }
            }
        }
        else {
            logger.log(Level.WARNING, "No elements found");
        }
    }

    private XPath setUpPath() {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return switch (prefix) {
                    case "uml" -> UML_NAMESPACE_URI;
                    case "xmi" -> XMI_NAMESPACE_URI;
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
        return xpath;
    }
}
