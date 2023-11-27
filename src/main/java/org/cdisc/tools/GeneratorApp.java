package org.cdisc.tools;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import net.steppschuh.markdowngenerator.table.Table;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * USDM Dictionary/Release Delta File Generator App
 * Depending on the arguments sent to this application, it will either generate
 * a markup table with the Data Dictionary
 * or generate a csv file ("output.csv") with the structural differences between
 * the actual and previous release of the
 * UML XMIs. In this latter case, each file should be placed in their
 * corresponding subfolders under "resources".
 * Additionally, a "cardinalities.json" file should be present to state
 * cardinalities that cannot currently be inferred
 * from the UML. This is a practise that should be discarded in the short term
 * in favor of model refactoring.
 * See "Run Configurations" in the IDEA Project to execute this application in
 * either mode.
 */
public class GeneratorApp {

    // Primary Input file names to generate Dictionary Table
    private static final String XML_FILE_NAME = "USDM_UML.xmi";
    private static final String CPT_FILE_NAME = "USDM_CT.xlsx";
    private static final String API_FILE_NAME = "USDM_API.json";
    // Folder names used to generate release differences
    private static final String PREV_RELEASE_FOLDER_NAME = "prevRelease/";
    private static final String CURR_RELEASE_FOLDER_NAME = "currentRelease/";
    // Cardinalities file
    private static final String CARDINALITY_JSON_FILE_NAME = "cardinalities.json";
    // The logger is configured in log4j2.xml. By default it will output to
    // dictionaryGenerator.log and the console
    private static final Logger logger = LoggerFactory.getLogger(GeneratorApp.class);

    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException,
            XPathExpressionException, OpenXML4JException, XmlException {
        if (args[0].equals("--gen-table")) {
            genTable();
        } else if (args[0].equals("--compare-releases")) {
            compareReleases();
        } else if (args[0].equals("--gen-structure")) {
            genStructure();
        }
        logger.info("All done");
    }

    private static void genTable() throws IOException, ParserConfigurationException, SAXException,
            XPathExpressionException, OpenXML4JException, XmlException {
        // Generate the Markdown for the Data Dictionary Table
        try (
                var currFile = GeneratorApp.class.getClassLoader()
                        .getResourceAsStream(CURR_RELEASE_FOLDER_NAME + XML_FILE_NAME);) {
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
            var type = new TypeToken<Map<String, IDCardinality>>() {
            }.getType();
            Map<String, IDCardinality> cardinalityMap = gson.fromJson(new FileReader(cardinalityFileUrl.getPath()),
                    type);
            logger.info("Finished processing files");
            logger.info("Moving on to Markdown Output");
            // Generate the markdown for documentation purposes
            Table.Builder tableBuilder = new Table.Builder()
                    .withAlignments(Table.ALIGN_LEFT, Table.ALIGN_CENTER, Table.ALIGN_LEFT, Table.ALIGN_LEFT,
                            Table.ALIGN_LEFT);
            tableBuilder.addRow("Class Name", "Attribute Name", "Data Type", "NCI C-Code", "Cardinality",
                    "Preferred Term", "Definition", "Codelist Ref", "Inherited From");
            allModelElements.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                // Class Row
                tableBuilder.addRow(entry.getValue().getName(), null, null, entry.getValue().getDefNciCode(),
                        null, entry.getValue().getPreferredTerm(), entry.getValue().getDefinition(), null);
                entry.getValue().getProperties().entrySet().forEach(propEntry -> {
                    // Property Rows
                    String cardinality = propEntry.getValue().getMultiplicity();
                    if (cardinality == null || cardinality == "") {
                        if (cardinalityMap.get(entry.getValue().getName()) != null)
                            cardinality = cardinalityMap.get(entry.getValue().getName()).getCardinalities()
                                    .getOrDefault(propEntry.getValue().getName(), null);
                    }
                    tableBuilder.addRow(null, propEntry.getValue().getName(),
                            propEntry.getValue().printType().replace("<", "\\<"),
                            propEntry.getValue().getDefNciCode(), cardinality,
                            propEntry.getValue().getPreferredTerm(),
                            propEntry.getValue().getDefinition(), propEntry.getValue().printCodeLists(),
                            propEntry.getValue().getInheritedFrom());

                });
            });
            Table table = tableBuilder.build();
            try (FileWriter out = new FileWriter("dataDictionary.MD")) {
                out.write(table.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println(table);
        }
    }

    private static void compareReleases() throws IOException, ParserConfigurationException, SAXException,
            XPathExpressionException, OpenXML4JException, XmlException {
        // Generate the Delta between releases
        try (
                var currFile = GeneratorApp.class.getClassLoader()
                        .getResourceAsStream(CURR_RELEASE_FOLDER_NAME + XML_FILE_NAME);
                var prevFile = GeneratorApp.class.getClassLoader()
                        .getResourceAsStream(PREV_RELEASE_FOLDER_NAME + XML_FILE_NAME);) {
            if (prevFile == null || currFile == null) {
                if (prevFile == null) {
                    logger.error("prevFile could not be found");
                }
                if (currFile == null) {
                    logger.error("currFile could not be found");
                }
                throw new RuntimeException("Input file not found");
            }
            UsdmParser parserPrev = new UsdmParser(prevFile);
            UsdmParser parserCurr = new UsdmParser(currFile);
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
    }

    private static class Relationship {
        private enum RelType {
            Value, Ref
        }

        private RelType type;
        private String name;

        private Relationship(RelType type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    private static void putIfNonEmpty(Map<String, Object> attribute, String name, Object value) {
        putIfNonEmpty(attribute, name, value, value);
    }

    private static void putIfNonEmpty(Map<String, Object> attribute, String name, Object check, Object value) {
        if (check == null || check.equals(false) || check.equals("")
                || (check instanceof Collection && ((Collection<?>) check).isEmpty())) {
            return;
        }
        attribute.put(name, value);
    }

    private static Relationship getRelatedAttribute(Map.Entry<String, ModelClassProperty> propEntry,
            Map<String, Map<String, ?>> classFromAPI) {
        if (classFromAPI != null) {
            List<Relationship> attributePaths = new ArrayList<>(
                    List.of(new Relationship(Relationship.RelType.Value, propEntry.getValue().getName()),
                            new Relationship(Relationship.RelType.Ref,
                                    propEntry.getValue().getName() + "Id"),
                            new Relationship(Relationship.RelType.Ref,
                                    propEntry.getValue().getName() + "Ids")));
            if (propEntry.getValue().getName().endsWith("s")) {
                attributePaths.add(new Relationship(Relationship.RelType.Ref,
                        propEntry.getValue().getName().substring(0,
                                propEntry.getValue().getName().length() - 1) + "Ids"));
            }
            if (propEntry.getValue().getName().endsWith("ies")) {
                attributePaths.add(new Relationship(Relationship.RelType.Ref,
                        propEntry.getValue().getName().substring(0,
                                propEntry.getValue().getName().length() - 3) + "yIds"));
            }
            for (Relationship attributePath : attributePaths) {
                try {
                    JsonPath.read(classFromAPI,
                            attributePath.name);
                    classFromAPI.remove(attributePath.name);
                    return attributePath;
                } catch (PathNotFoundException e) {
                }
            }
        }
        return null;
    }

    static final String UNKNOWN = "UNKNOWN";

    private static void buildNonModeledAttributes(Map<String, Map<String, ?>> classFromAPI, Object jsonDocument,
            Map<String, Object> attributes) {
        if (classFromAPI != null) {
            for (Map.Entry<String, Map<String, ?>> propEntry : classFromAPI.entrySet()) {
                Map<String, String> attribute = new LinkedHashMap<>();
                attributes.put(propEntry.getKey(), attribute);
                if (propEntry.getValue().containsKey("type") && propEntry.getValue().get("type").equals("array")) {
                    String ref = ((Map<String, String>) propEntry.getValue().get("items")).get("$ref");
                    if (ref == null) {
                        attribute.put("$ref", UNKNOWN);
                    } else {
                        ref = ref.replaceFirst("\\#", "\\$");
                        ref = ref.replaceAll("\\/", "\\.");
                        try {
                            Map<String, ?> def = JsonPath.read(jsonDocument, ref);
                            attribute.put("$ref", "#/" + def.get("title"));
                        } catch (PathNotFoundException e) {
                            attribute.put("$ref", UNKNOWN);
                        }
                    }
                    attribute.put("Cardinality", "0..*");
                } else if (propEntry.getValue().containsKey("type")
                        && propEntry.getValue().get("type").equals("string")) {
                    attribute.put("$ref", "#/string");
                    attribute.put("Cardinality", "0..1");
                } else {
                    attribute.put("$ref", UNKNOWN);
                }
                attribute.put("Relationship Type", "Value");
            }
        }
    }

    private static void genStructure() throws IOException, ParserConfigurationException, SAXException,
            XPathExpressionException, OpenXML4JException, XmlException {
        // Generate the Markdown for the Data Dictionary Table

        try (
                var currFile = GeneratorApp.class.getClassLoader()
                        .getResourceAsStream(CURR_RELEASE_FOLDER_NAME + XML_FILE_NAME);
                var currAPIFile = GeneratorApp.class.getClassLoader()
                        .getResourceAsStream(API_FILE_NAME);) {
            // Process the Main UML XMI Model first
            UsdmParser usdmParser = new UsdmParser(currFile);
            Object jsonDocument = Configuration.defaultConfiguration().jsonProvider()
                    .parse(new String(currAPIFile.readAllBytes()));
            // allModelElements contains a deserialized representation of the UML
            Map<String, ModelClass> allModelElements = new TreeMap<>();
            usdmParser.loadFromUsdmXmi(allModelElements);
            if (allModelElements.isEmpty()) {
                throw new RuntimeException("Possible Usdm XMI Parsing Error. Check file and structure");
            }
            // Next, add more detailed information from the CT Spreadsheet
            CptParser cptParser = new CptParser(CPT_FILE_NAME);
            cptParser.populateMapwithCpt(allModelElements);
            Map<String, Map<String, Object>> classes = new TreeMap<>();
            for (Map.Entry<String, ModelClass> entry : allModelElements.entrySet()) {
                Map<String, Object> attributes = new LinkedHashMap<>();
                Map<String, Object> clazz = new LinkedHashMap<>();
                Map<String, Map<String, ?>> classFromAPI = null;
                List<String> classPaths = List.of("$.components.schemas." + entry.getValue().getName() + ".properties",
                        "$.components.schemas." + entry.getValue().getName() + "-Output.properties");
                for (String classPath : classPaths) {
                    try {
                        classFromAPI = JsonPath.read(jsonDocument,
                                classPath);
                        break;
                    } catch (PathNotFoundException e) {
                    }
                }
                putIfNonEmpty(clazz, "NCI C-Code", entry.getValue().getDefNciCode());
                putIfNonEmpty(clazz, "Preferred Term", entry.getValue().getPreferredTerm());
                putIfNonEmpty(clazz, "Definition", entry.getValue().getDefinition());
                putIfNonEmpty(clazz, "Super Classes",
                        entry.getValue().getSuperClasses().stream().map((value) -> Map.of("$ref", "#/" + value))
                                .collect(Collectors.toList()));
                putIfNonEmpty(clazz, "Sub Classes",
                        entry.getValue().getSubClasses().stream().map((value) -> Map.of("$ref", "#/" + value))
                                .collect(Collectors.toList()));
                clazz.put("Modifier", classFromAPI == null ? "Abstract" : "Concrete");
                clazz.put("Attributes", attributes);
                classes.put(entry.getValue().getName(), clazz);
                for (Map.Entry<String, ModelClassProperty> propEntry : entry.getValue().getProperties().entrySet()) {
                    Map<String, Object> attribute = new LinkedHashMap<>();
                    putIfNonEmpty(attribute, "$ref", "#/" + propEntry.getValue().printType().replace("<", "\\<"));
                    putIfNonEmpty(attribute, "NCI C-Code", propEntry.getValue().getDefNciCode());
                    putIfNonEmpty(attribute, "Cardinality", propEntry.getValue().getMultiplicity());
                    putIfNonEmpty(attribute, "Preferred Term", propEntry.getValue().getPreferredTerm());
                    putIfNonEmpty(attribute, "Definition", propEntry.getValue().getDefinition());
                    putIfNonEmpty(attribute, "Codelist Ref", propEntry.getValue().printCodeLists());
                    Relationship attributeFromAPI = getRelatedAttribute(propEntry, classFromAPI);
                    putIfNonEmpty(attribute, "Relationship Type", classFromAPI,
                            attributeFromAPI == null ? UNKNOWN : attributeFromAPI.type.toString());
                    attribute.put("Model Name", propEntry.getValue().getName());
                    putIfNonEmpty(attribute, "Inherited From", propEntry.getValue().getInheritedFrom(),
                            Map.of("$ref", "#/" + propEntry.getValue().getInheritedFrom()));
                    attributes.put(
                            attributeFromAPI == null
                                    ? propEntry.getValue().getName() + (classFromAPI == null ? "" : "*")
                                    : attributeFromAPI.name,
                            attribute);
                }
                buildNonModeledAttributes(classFromAPI, jsonDocument, attributes);
            }
            logger.info("Finished processing files");
            logger.info("Moving on to YAML Output");
            // Generate the yaml for documentation purposes
            DumperOptions dumperOptions = new DumperOptions();
            dumperOptions.setDefaultFlowStyle(FlowStyle.BLOCK);
            Yaml yaml = new Yaml(dumperOptions);
            FileWriter writer = new FileWriter("dataStructure.yml");
            yaml.dump(classes, writer);
        }
    }
}
