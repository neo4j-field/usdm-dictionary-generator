package org.cdisc.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class UsdmParser {

    private static final Logger logger = LoggerFactory.getLogger(UsdmParser.class);

    private Document document = null;
    private Set<String> namespaces = null;

    public UsdmParser(InputStream file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        file.transferTo(baos);
        this.document = builder.parse(new ByteArrayInputStream(baos.toByteArray()));
        try {
            this.namespaces = setUpNamespaces(new ByteArrayInputStream(baos.toByteArray()));
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }

    }

    public void loadFromUsdmXmi(Map<String, ModelClass> elements) throws XPathExpressionException {
        logger.debug("ENTER - loadFromUsdmXmi");
        XPath xPath = setUpPath();
        XPathExpression expr = xPath.compile("//xmi:XMI/uml:Model//packagedElement[@xmi:type='uml:Class']");
//        XPathExpression expr = xPath.compile("//*[local-name()='uml:Class']");

        Object result = expr.evaluate(document, XPathConstants.NODESET);
        NodeList nodes = (NodeList) result;
        if (nodes.getLength() > 0) {
            logger.debug("Total Classes Found: " + nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                Node currentItem = nodes.item(i);
                Map<String, ModelClassProperty> properties = new HashMap<>();
                String className = currentItem.getAttributes().getNamedItem("name").getNodeValue();
                elements.put(className, new ModelClass(className, properties, null));
                ////xmi:XMI/uml:Model//packagedElement[@name='Activity']/ownedAttribute[@xmi:type='uml:Property']
//                String propertyExprStr = String.format("//xmi:XMI/uml:Model//packagedElement[@name='%1$s']/" +
//                        "ownedAttribute[@xmi:type='uml:Property']", className);
                String propertyExprStr = String.format("//xmi:XMI/uml:Model//packagedElement[@name='%1$s']/" +
                        "ownedAttribute[@xmi:type='uml:Property']", className);
                XPathExpression propertyExpr = xPath.compile(propertyExprStr);
                Object propsResult = propertyExpr.evaluate(this.document, XPathConstants.NODESET);
                NodeList propsNodes = (NodeList) propsResult;
                logger.debug("Total Properties Found: " + propsNodes.getLength());
                for (int j = 0; j < propsNodes.getLength(); j++) {
                    Node currentProp = propsNodes.item(j);
                    String propName = currentProp.getAttributes().getNamedItem("name").getNodeValue();
                    logger.debug(String.format("Pulling propertyAttributes for %1$s", propName));
                    String propId = currentProp.getAttributes().getNamedItem("xmi:id").getNodeValue();
                    String propertyTypeExprStr = String.format("//attribute[@xmi:idref='%1$s']", propId);
                    XPathExpression propertyTypeExpr = xPath.compile(propertyTypeExprStr);
                    Object propTypesResult = propertyTypeExpr.evaluate(this.document, XPathConstants.NODESET);
                    NodeList propTypesNodes = (NodeList) propTypesResult;
                    if (!properties.containsKey(propName)) {
                        String propType = propTypesNodes.item(0).getChildNodes()
                                .item(7).getAttributes().getNamedItem("type").getNodeValue();
                        logger.debug(String.format("Found propType %1$s for %2$s", propType, propName));
                        properties.put(propName, new ModelClassProperty(propName, propType, null, null));
                    }
                    if (propTypesNodes.getLength() == 0) {
                        logger.warn(String.format("Ignoring duplicate property in UML XMI: %1$s", propName));
                    }

                }
            }
        }
        else {
            logger.warn(String.format("%1$s: No elements found",document.getDocumentURI()));
        }
        logger.debug("LEAVE - loadFromUsdmXmi");
    }

    private XPath setUpPath() {
        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                Map<String, String> uris = new HashMap<>();
                List<String[]> names = namespaces.stream().map(item -> item.split(", ")).toList();
                for (String[] name: names) {
                    uris.put(name[1].trim(), name[2].trim());
                }
                String result = uris.getOrDefault(prefix, null);
                return result;
            }

            public Iterator<String> getPrefixes(String val) {
                return null;
            }

            public String getPrefix(String uri) {
                return null;
            }
        });
        return xPath;
    }

    private Set<String> setUpNamespaces(InputStream file) throws XMLStreamException {
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = inputFactory.createXMLStreamReader(file);
        Set<String> namespaces = new HashSet<>();
        while (reader.hasNext()) {
            int evt = reader.next();
            if (evt == XMLStreamConstants.START_ELEMENT) {
                QName qName = reader.getName();
                if(qName != null){
                    if(qName.getPrefix() != null && qName.getPrefix().compareTo("")!=0)
                        namespaces.add(String.format("%s, %s, %s",
                                qName.getLocalPart(), qName.getPrefix(), qName.getNamespaceURI()));
                }
            }
        }
        return namespaces;
    }
}
