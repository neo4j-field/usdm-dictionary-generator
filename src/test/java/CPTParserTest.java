import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.extractor.XSSFEventBasedExcelExtractor;
import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CPTParserTest {

    private static String testFileName = "ct-simple.xlsx";
    private static String tmp_output = "target/test-classes/output.csv";
    private static final Logger logger = Logger.getLogger(CPTParserTest.class.getName());


    @Test
    public void shouldReadXlsandCreateCsv() throws IOException, OpenXML4JException, XmlException {
        var url = CPTParserTest.class.getResource(testFileName);

        CSVPrinter printer = new CSVPrinter(new FileWriter(tmp_output), CSVFormat.RFC4180);

        try (XSSFEventBasedExcelExtractor tsv = new XSSFEventBasedExcelExtractor(url.toURI().getPath())) {
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
        }
    }


    @Test
    public void shouldParseCsv() throws IOException {
//        CSVParser parser = CSVParser.parse(new File(tmp_output), StandardCharsets.UTF_8,
//                CSVFormat.TDF.withHeader("Row #","Entity Name", "Role",
//                        "Logical Data Model Name", "NCI C-code", "CT Item Preferred Name", "Synonym(s)",
//                        "Definition", "Has Value List"));
        Reader reader = new InputStreamReader(new BOMInputStream(CPTParserTest.class.getResourceAsStream("output.csv")),
                StandardCharsets.UTF_8);
        try (reader; CSVParser parser = new CSVParser(reader, CSVFormat.RFC4180)) {
            for (final CSVRecord record : parser) {
                    logger.log(Level.INFO, record.toString());

            }
        }
    }

}
