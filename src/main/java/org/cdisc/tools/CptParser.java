package org.cdisc.tools;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.extractor.XSSFEventBasedExcelExtractor;
import org.apache.xmlbeans.XmlException;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CptParser {

    private static final Logger logger = Logger.getLogger(CptParser.class.getName());

    private String inputFileName;
    private String tmpCsvFileName;

    public CptParser(String inputFileName) throws OpenXML4JException, XmlException, IOException {
        this.inputFileName = inputFileName;
        try {
            var tmpUrl = CptParser.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            tmpCsvFileName = tmpUrl.getPath() + "output.csv";
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        generateCsvFileFromCptSpreadsheet();
    }


    private void generateCsvFileFromCptSpreadsheet() throws IOException, OpenXML4JException, XmlException {
        var url = CptParser.class.getClassLoader().getResource(this.inputFileName);

        try (XSSFEventBasedExcelExtractor tsv = new XSSFEventBasedExcelExtractor(url.toURI().getPath())) {
            CSVPrinter printer = new CSVPrinter(new FileWriter(tmpCsvFileName), CSVFormat.RFC4180);
            // We need to include sheet names so that we can skip the stuff we don't need
            tsv.setIncludeSheetNames(true);
            tsv.setIncludeCellComments(false);
            tsv.setIncludeTextBoxes(false);
            tsv.setIncludeHeadersFooters(false);

            String[] contents = tsv.getText().split("\n");
            for (String record: contents) {
                // Break the loop as soon as we find the worksheet we don't care about
                if (record.contains("DDF valid value sets")) {
                    break;
                }
                String[] rowElements = record.split("\t");
                for (String element: rowElements) {
                    printer.print(element);
                }
                printer.println();
                printer.flush();
            }
            printer.close();
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public void populateMapwithCpt(Map<String, ModelClass> modelElements) throws IOException {
        Reader reader = new InputStreamReader(new BOMInputStream(new FileInputStream((tmpCsvFileName))), StandardCharsets.UTF_8);
        try (reader; CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.withSkipHeaderRecord())) {
            for (final CSVRecord record : parser) {
                if (record.size() >= 3) {
                    String elementType = record.get(2);

                    ModelClass modelClass = modelElements.get(record.get(1));
                    // These null checks are necessary to cover inconsistencies between XMI and CT Spreadsheet
                    if (modelClass != null) {
                        if (elementType.equals("Entity")) {
                            if (record.size() >= 8) modelClass.setDescription(record.get(6));
                        }
                        else if (elementType.equals("Relationship") || elementType.equals("Attribute")) {
                            ModelClassProperty property = modelElements.get(record.get(1)).getProperties().get(record.get(3));
                            if (property != null) {
                                if (record.size() >= 8) property.setDescription(record.get(6));

                            }
                            else {
                                logger.log(Level.WARNING, "Could not find Property: " + record.get(3));
                            }

                        }

                    }
                    else {
                        logger.log(Level.WARNING, "Could not find Class: " + record.get(1));
                    }
                }
                logger.log(Level.FINE, record.toString());
            }

        }
    }
}
