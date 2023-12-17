package org.cdisc.tools;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tools to generate detailed information from the CPT Spreadsheet, and populate
 * ModelClass with it
 * There can be situations in which this process will fail, especially if the
 * spreadsheet contains formatting data,
 * or anything that cannot be interpreted by apache poi directly. In such cases
 * the spreadsheet itself has been fixed
 * before submitted to this process.
 */
public class CptParser {

    private static final Logger logger = LoggerFactory.getLogger(CptParser.class);

    private String inputFileName;
    private String tmpCsvFileName;

    private enum Column {
        ROW_NUMBER,
        ENTITY_NAME,
        ROLE,
        INHERITED_FROM,
        LOGICAL_DATA_MODEL_NAME,
        NCI_C_CODE,
        CT_ITEM_PREFERRED_NAME,
        SYNONYMS,
        DEFINITION,
        HAS_VALUE_LIST,
        CODELIST_URL
    }

    public CptParser(String inputFileName) throws OpenXML4JException, XmlException, IOException {
        this.inputFileName = inputFileName;
        try {
            var tmpUrl = CptParser.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            // A temp csv file will always be generated as part of parsing the incoming
            // Excel file
            tmpCsvFileName = tmpUrl.getPath() + "output.csv";
            generateCsvFileFromCptSpreadsheet();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    private void generateCsvFileFromCptSpreadsheet() throws URISyntaxException, IOException, InvalidFormatException {
        var url = CptParser.class.getClassLoader().getResource(this.inputFileName);
        XSSFWorkbook workbook = new XSSFWorkbook(new File(url.toURI().getPath()));
        CSVPrinter printer = new CSVPrinter(new FileWriter(tmpCsvFileName), CSVFormat.RFC4180.withDelimiter(','));

        String extractor = new XSSFExcelExtractor(workbook).getText();
        BufferedReader reader = new BufferedReader(new StringReader(extractor));
        reader.lines().map(line -> line.split("\t")).forEach(element -> {
            try {
                printer.printRecord(element);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * This is the primary method, in charge of populating the ModelClass instance
     * with details from the spreadsheet
     * 
     * @param modelElements
     * @throws IOException
     */
    public void populateMapwithCpt(Map<String, ModelClass> modelElements) throws IOException {
        logger.debug("ENTER - populateMapwithCpt");
        Reader reader = new InputStreamReader(new BOMInputStream(new FileInputStream((tmpCsvFileName))),
                StandardCharsets.UTF_8);
        try (reader;
                CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.withSkipHeaderRecord().withDelimiter(','))) {
            for (final CSVRecord record : parser) {
                if (record.size() > Column.ROLE.ordinal()) {
                    String elementType = record.get(Column.ROLE.ordinal());

                    ModelClass modelClass = modelElements.get(record.get(Column.ENTITY_NAME.ordinal()));
                    // These null checks are necessary to cover inconsistencies between XMI and CT
                    // Spreadsheet
                    if (modelClass != null) {
                        if (elementType.equals("Entity")) {
                            if (record.size() > Column.DEFINITION.ordinal())
                                modelClass.setDefinition(record.get(Column.DEFINITION.ordinal()));
                            modelClass.setPreferredTerm(record.get(Column.CT_ITEM_PREFERRED_NAME.ordinal()));
                            modelClass.setDefNciCode(record.get(Column.NCI_C_CODE.ordinal()));
                        } else if (elementType.equals("Relationship") || elementType.equals("Attribute")
                                || elementType.equals("Complex Datatype Relationship")) {
                            ModelClassProperty property = modelElements
                                    .get(record.get(Column.ENTITY_NAME.ordinal())).getProperties()
                                    .get(record.get(Column.LOGICAL_DATA_MODEL_NAME.ordinal()));
                            if (property != null) {
                                if (record.size() > Column.DEFINITION.ordinal()) {
                                    // Update Description and CodeList references
                                    property.setDefinition(record.get(Column.DEFINITION.ordinal()));
                                    property.setPreferredTerm(record.get(Column.CT_ITEM_PREFERRED_NAME.ordinal()));
                                    property.setDefNciCode(record.get(Column.NCI_C_CODE.ordinal()));
                                    if (record.get(Column.HAS_VALUE_LIST.ordinal()).trim().toUpperCase()
                                            .contains("Y")) {
                                        String codeListRef = record.get(Column.HAS_VALUE_LIST.ordinal())
                                                .replace("Y", "").trim();
                                        property.setCodeListReference(List.of(codeListRef));
                                    }
                                }
                            } else {
                                logger.warn("Could not find Property: "
                                        + record.get(Column.LOGICAL_DATA_MODEL_NAME.ordinal()));
                            }
                        }
                    } else {
                        logger.warn("Could not find Class: " + record.get(Column.ENTITY_NAME.ordinal()));
                    }
                }
                logger.debug(record.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        logger.debug("LEAVE - populateMapwithCpt");
    }

    public Map<String, ModelClass> getEntitiesMap() throws FileNotFoundException {
        logger.debug("ENTER - Get CSV Entities");
        Map<String, ModelClass> elements = new TreeMap<>();
        Reader reader = new InputStreamReader(new BOMInputStream(new FileInputStream((tmpCsvFileName))),
                StandardCharsets.UTF_8);
        try (reader;
                CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.withSkipHeaderRecord().withDelimiter(','))) {
            for (final CSVRecord record : parser) {
                if (record.size() > Column.ROLE.ordinal()) {
                    // These null checks are necessary to cover inconsistencies between XMI and CT
                    // Spreadsheet
                    String className = record.get(Column.ENTITY_NAME.ordinal());
                    String elementType = record.get(Column.ROLE.ordinal());
                    if (elementType.equals("Entity")) {
                        ModelClass modelClass = new ModelClass(className, new LinkedHashMap<>(), null);
                        elements.put(className, modelClass);
                        if (record.size() > Column.DEFINITION.ordinal())
                            modelClass.setDefinition(record.get(Column.DEFINITION.ordinal()));
                        modelClass.setPreferredTerm(record.get(Column.CT_ITEM_PREFERRED_NAME.ordinal()));
                        modelClass.setDefNciCode(record.get(Column.NCI_C_CODE.ordinal()));
                    } else if (elementType.equals("Relationship") || elementType.equals("Attribute")
                            || elementType.equals("Complex Datatype Relationship")) {
                        String propertyName = record.get(Column.LOGICAL_DATA_MODEL_NAME.ordinal());
                        ModelClassProperty property = new ModelClassProperty(propertyName, null, null, null, null,
                                null);
                        elements.get(className).getProperties().put(propertyName, property);
                        if (record.size() > Column.DEFINITION.ordinal()) {
                            // Update Description and CodeList references
                            property.setDefinition(record.get(Column.DEFINITION.ordinal()));
                            property.setPreferredTerm(record.get(Column.CT_ITEM_PREFERRED_NAME.ordinal()));
                            property.setDefNciCode(record.get(Column.NCI_C_CODE.ordinal()));
                            if (record.get(Column.HAS_VALUE_LIST.ordinal()).trim().toUpperCase().contains("Y")) {
                                String codeListRef = record.get(Column.HAS_VALUE_LIST.ordinal()).replace("Y", "")
                                        .trim();
                                property.setCodeListReference(List.of(codeListRef));
                            }
                        }

                    }
                }
                logger.debug(record.toString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        logger.debug("LEAVE - Get CSV Entities");
        return elements;
    }
}
