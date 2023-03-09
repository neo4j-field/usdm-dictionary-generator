package org.cdisc.tools;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.extractor.XSSFEventBasedExcelExtractor;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlException;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CptParser {

    private static final Logger logger = LoggerFactory.getLogger(CptParser.class);

    private String inputFileName;
    private String tmpCsvFileName;

    public CptParser(String inputFileName) throws OpenXML4JException, XmlException, IOException {
        this.inputFileName = inputFileName;
        try {
            var tmpUrl = CptParser.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            tmpCsvFileName = tmpUrl.getPath() + "output.csv";
            generateCsvFileFromCptSpreadsheet(true);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }

    private void generateCsvFileFromCptSpreadsheet(boolean userModel) throws URISyntaxException, IOException, InvalidFormatException {
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

    private void generateCsvFileFromCptSpreadsheet() throws IOException, OpenXML4JException, XmlException {
        var url = CptParser.class.getClassLoader().getResource(this.inputFileName);

        try (XSSFEventBasedExcelExtractor tsv = new XSSFEventBasedExcelExtractor(url.toURI().getPath())) {
            CSVPrinter printer = new CSVPrinter(new FileWriter(tmpCsvFileName), CSVFormat.RFC4180.withDelimiter(','));
            // We need to include sheet names so that we can skip the stuff we don't need
            tsv.setIncludeCellComments(false);
            tsv.setLocale(Locale.US);

            String[] contents = tsv.getText().split("\n");
            for (String record: contents) {
                // Break the loop as soon as we find the worksheet we don't care about
                if (record.contains("DDF valid value sets")) {
                    break;
                }
                String[] rowElements = record.replace("\u00a0","").split("\t");
                for (String element: rowElements) {
                    printer.print(element);
                }
                printer.println();
                printer.flush();
            }
            printer.close();
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage());
        }
    }

    public void populateMapwithCpt(Map<String, ModelClass> modelElements) throws IOException {
        logger.debug("ENTER - populateMapwithCpt");
        Reader reader = new InputStreamReader(new BOMInputStream(new FileInputStream((tmpCsvFileName))), StandardCharsets.UTF_8);
        try (reader; CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180.withSkipHeaderRecord().withDelimiter(','))) {
            for (final CSVRecord record : parser) {
                if (record.size() >= 3) {
                    String elementType = record.get(2);

                    ModelClass modelClass = modelElements.get(record.get(1));
                    // These null checks are necessary to cover inconsistencies between XMI and CT Spreadsheet
                    if (modelClass != null) {
                        if (elementType.equals("Entity")) {
                            if (record.size() >= 8) modelClass.setDescription(record.get(7));
                        }
                        else if (elementType.equals("Relationship") || elementType.equals("Attribute")) {
                            ModelClassProperty property = modelElements.get(record.get(1)).getProperties().get(record.get(3));
                            if (property != null) {
                                if (record.size() >= 8)  {
                                    // Update Description and CodeList references
                                    property.setDescription(record.get(7));
                                    if (record.get(8).trim().toUpperCase().contains("Y")) {
                                        String codeListRef = record.get(8).replace("Y","").trim();
                                        property.setCodeListReference(List.of(codeListRef));
                                    }
                                }
                            }
                            else {
                                logger.warn("Could not find Property: " + record.get(3));
                            }
                        }
                    }
                    else {
                        logger.warn("Could not find Class: " + record.get(1));
                    }
                }
                logger.debug(record.toString());
            }
        }
        logger.debug("LEAVE - populateMapwithCpt");
    }
}
