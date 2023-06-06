package org.cdisc.tools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.steppschuh.markdowngenerator.table.Table;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** USDM Dictionary/Release Delta File Generator App
 * Depending on the arguments sent to this application, it will either generate a markup table with the Data Dictionary
 * or generate a csv file ("output.csv") with the structural differences between the actual and previous release of the
 * UML XMIs. In this latter case, each file should be placed in their corresponding subfolders under "resources".
 * Additionally, a "cardinalities.json" file should be present to state cardinalities that cannot currently be inferred
 * from the UML. This is a practise that should be discarded in the short term in favor of model refactoring.
 * See "Run Configurations" in the IDEA Project to execute this application in either mode.
 */
public class GeneratorApp {

    // Primary Input file names to generate Dictionary Table
    private static final String XML_FILE_NAME = "USDM_UML.xmi";
    private static final String CPT_FILE_NAME = "USDM_CT.xlsx";
    // Folder names used to generate release differences
    private static final String PREV_RELEASE_FOLDER_NAME = "prevRelease/";
    private static final String CURR_RELEASE_FOLDER_NAME = "currentRelease/";
    // Cardinalities file
    private static final String CARDINALITY_JSON_FILE_NAME = "cardinalities.json";
    // The logger is configured in log4j2.xml. By default it will output to dictionaryGenerator.log and the console
    private static final Logger logger = LoggerFactory.getLogger(GeneratorApp.class);

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException,
            XPathExpressionException, OpenXML4JException, XmlException {

        try (
                var currFile = GeneratorApp.class.getClassLoader().getResourceAsStream(CURR_RELEASE_FOLDER_NAME +XML_FILE_NAME);
        ) {
            // Generate the Markdown for the Data Dictionary Table
            if (args[0].equals("--gen-table")) {
                // Process the Main UML XMI Model first
                UsdmParser usdmParser = new UsdmParser(currFile);
                // allModelElements contains a deserialized representation of the UML
                Map<String, ModelClass> allModelElements = new HashMap<>();
                usdmParser.loadFromUsdmXmi(allModelElements);
                if (allModelElements.isEmpty()) {
                    throw new RuntimeException("Possible Usdm XMI Parsing Error. Check file and structure");
                }
                // Next, add more detailed information from the CT Spreadsheet
                CptParser cptParser = new CptParser(CPT_FILE_NAME);
                cptParser.populateMapwithCpt(allModelElements);
                // Pull additional cardinalities from json file
                Gson gson = new Gson();
                var cardinalityFileUrl = GeneratorApp.class.getClassLoader().getResource(CARDINALITY_JSON_FILE_NAME);
                var type = new TypeToken<Map<String, IDCardinality>>(){}.getType();
                Map<String, IDCardinality> cardinalityMap = gson.fromJson(new FileReader(cardinalityFileUrl.getPath()), type);
                logger.info("Finished processing files");
                logger.info("Moving on to Markdown Output");
                // Generate the markdown for documentation purposes
                Table.Builder tableBuilder = new Table.Builder()
                        .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT, Table.ALIGN_LEFT, Table.ALIGN_LEFT);
                tableBuilder.addRow("Class Name", "Attribute Name", "Data Type", "NCI C-Code", "Cardinality", "Preferred Term", "Definition", "Codelist Ref");
                allModelElements.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                    // Class Row
                    tableBuilder.addRow(entry.getValue().getName(), null, null, entry.getValue().getDefNciCode(),
                            null, entry.getValue().getPreferredTerm(), entry.getValue().getDefinition(), null);
                    entry.getValue().getProperties().entrySet().forEach(propEntry -> {
                        // Property Rows
                        String cardinality = propEntry.getValue().getMultiplicity();
                        if (cardinality == null || cardinality == "") {
                            if (cardinalityMap.get(entry.getValue().getName()) != null)
                                cardinality = cardinalityMap.get(entry.getValue().getName()).getCardinalities().getOrDefault(propEntry.getValue().getName(), null);
                        }
                        tableBuilder.addRow(null, propEntry.getValue().getName(),
                                propEntry.getValue().printType().replace("<", "\\<"),
                                propEntry.getValue().getDefNciCode(), cardinality,
                                propEntry.getValue().getPreferredTerm(),
                                propEntry.getValue().getDefinition(), propEntry.getValue().printCodeLists());
                    });
                });
                Table table = tableBuilder.build();
                try (FileWriter out = new FileWriter("dataDictionary.MD")){
                    out.write(table.toString());
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(table);
            } else if (args[0].equals("--compare-releases")) {
                // Generate the Delta between releases
                var prevFile = GeneratorApp.class.getClassLoader().getResourceAsStream(PREV_RELEASE_FOLDER_NAME + XML_FILE_NAME);
                //var currFile = GeneratorApp.class.getClassLoader().getResourceAsStream(CURR_RELEASE_FOLDER_NAME + XML_FILE_NAME);

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
