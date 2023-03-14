package org.cdisc.tools;

import net.steppschuh.markdowngenerator.table.Table;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class GeneratorApp {

    private static final String XML_FILE_NAME = "USDM_UML.xmi";
    private static final String CPT_FILE_NAME = "USDM_CT.xlsx";

    private static final String PREV_RELEASE_FOLDER_NAME = "prevRelease/";
    private static final String CURR_RELEASE_FOLDER_NAME = "currentRelease/";

    private static final Logger logger = LoggerFactory.getLogger(GeneratorApp.class);

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException,
            XPathExpressionException, OpenXML4JException, XmlException {

        try (
                var file = GeneratorApp.class.getClassLoader().getResourceAsStream(XML_FILE_NAME);
        ) {

            if (args[0].equals("--gen-table")) {

                // Process the UML XMI Model first
                // TODO - MUST RECHECK!!!
                UsdmParser usdmParser = new UsdmParser(file);
                Map<String, ModelClass> allModelElements = new HashMap<>();
                usdmParser.loadFromUsdmXmi(allModelElements);
                if (allModelElements.isEmpty()) {
                    throw new RuntimeException("Possible Usdm XMI Parsing Error. Check file and structure");
                }
                // Complete with information from the CT Spreadsheet
                CptParser cptParser = new CptParser(CPT_FILE_NAME);
                cptParser.populateMapwithCpt(allModelElements);
                logger.info("Finished processing files");
                logger.info("Moving on to Markdown Output");
                // Generate the markdown for documentation purposes
                Table.Builder tableBuilder = new Table.Builder()
                        .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT, Table.ALIGN_LEFT, Table.ALIGN_LEFT);
                tableBuilder.addRow("Class Name", "Property Name", "Type", "Description", "Codelist Ref");
                allModelElements.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                    // Class Row
                    tableBuilder.addRow(entry.getValue().getName(), null, null, entry.getValue().getDescription(), null);
                    entry.getValue().getProperties().entrySet().forEach(propEntry -> {
                        // Property Rows
                        tableBuilder.addRow(null, propEntry.getValue().getName(),
                                propEntry.getValue().getType().replace("<", "\\<"),
                                propEntry.getValue().getDescription(), propEntry.getValue().getCodeListReference());
                    });
                });
                System.out.println(tableBuilder.build());
            } else if (args[0].equals("--compare-releases")) {
                var prevFile = GeneratorApp.class.getClassLoader().getResourceAsStream(PREV_RELEASE_FOLDER_NAME + XML_FILE_NAME);
                var currFile = GeneratorApp.class.getClassLoader().getResourceAsStream(CURR_RELEASE_FOLDER_NAME + XML_FILE_NAME);

                if (prevFile == null || currFile == null) {
                    logger.error("One of the input files could not be found");
                    throw new RuntimeException("Input file not found");
                }
                UsdmParser parserPrev = new UsdmParser(prevFile);
                prevFile.close();
                UsdmParser parserCurr = new UsdmParser(currFile);
                currFile.close();
                Map<String, ModelClass> prevModel = new HashMap<>();
                parserPrev.loadFromUsdmXmi(prevModel);
                Map<String, ModelClass> currModel = new HashMap<>();
                parserCurr.loadFromUsdmXmi(currModel);

                if (prevModel.isEmpty() || currModel.isEmpty()) {
                    logger.error("Possible problem with parsing one of the XMI's namespaces");
                    throw new RuntimeException("Cannot perform XMI Comparison");
                }

                Table.Builder diffs = Utils.printDifferences(prevModel, currModel);
                System.out.println(diffs.build());
            }
            logger.info("All done");
        }
    }
}
