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
                if (record.size() >= 3) {
                    String elementType = record.get(2);

                    ModelClass modelClass = modelElements.get(record.get(1));
                    // These null checks are necessary to cover inconsistencies between XMI and CT
                    // Spreadsheet
                    if (modelClass != null) {
                        if (elementType.equals("Entity")) {
                            if (record.size() >= 8)
                                modelClass.setDefinition(record.get(7));
                            modelClass.setPreferredTerm(record.get(5));
                            modelClass.setDefNciCode(record.get(4));
                        } else if (elementType.equals("Relationship") || elementType.equals("Attribute")) {
                            ModelClassProperty property = modelElements.get(record.get(1)).getProperties()
                                    .get(record.get(3));
                            if (property != null) {
                                if (record.size() >= 8) {
                                    // Update Description and CodeList references
                                    property.setDefinition(record.get(7));
                                    property.setPreferredTerm(record.get(5));
                                    property.setDefNciCode(record.get(4));
                                    if (record.get(8).trim().toUpperCase().contains("Y")) {
                                        String codeListRef = record.get(8).replace("Y", "").trim();
                                        property.setCodeListReference(List.of(codeListRef));
                                    }
                                }
                            } else {
                                logger.warn("Could not find Property: " + record.get(3));
                            }
                        }
                    } else {
                        logger.warn("Could not find Class: " + record.get(1));
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
                if (record.size() >= 3) {
                    // These null checks are necessary to cover inconsistencies between XMI and CT
                    // Spreadsheet
                    String className = record.get(1);
                    String elementType = record.get(2);
                    if (elementType.equals("Entity")) {
                        ModelClass modelClass = new ModelClass(className, new LinkedHashMap<>(), null);
                        elements.put(className, modelClass);
                        if (record.size() >= 8)
                            modelClass.setDefinition(record.get(7));
                        modelClass.setPreferredTerm(record.get(5));
                        modelClass.setDefNciCode(record.get(4));
                    } else if (elementType.equals("Relationship") || elementType.equals("Attribute")) {
                        String propertyName = record.get(3);
                        ModelClassProperty property = new ModelClassProperty(propertyName, null, null, null, null);
                        elements.get(className).getProperties().put(propertyName, property);
                        if (record.size() >= 8) {
                            // Update Description and CodeList references
                            property.setDefinition(record.get(7));
                            property.setPreferredTerm(record.get(5));
                            property.setDefNciCode(record.get(4));
                            if (record.get(8).trim().toUpperCase().contains("Y")) {
                                String codeListRef = record.get(8).replace("Y", "").trim();
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
