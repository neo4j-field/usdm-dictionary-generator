import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Iterator;

public class OldCsvProcessingTest {

    private static final String NEW_LINE_CHARACTER = "\r\n";
    private static final String CVS_SEPERATOR_CHAR = ",";
    private static final String OUTPUT_DATE_FORMAT = "yyyy-MM-dd";

    private static String testFileName = "ct-simple.xlsx";
    private static String tmp_output = "target/test-classes/output.csv";

    @Test
    @Disabled
    public void oldProcess() throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(CPTParserTest.class.getResourceAsStream(testFileName));
        XSSFSheet sheet = workbook.getSheetAt(0);
        Iterator rowIter = sheet.rowIterator();
        String csvData = "";
        while (rowIter.hasNext()) {
            XSSFRow row = (XSSFRow) rowIter.next();
            for (int i = 0; i < row.getLastCellNum(); i++) {
                csvData += getCellData(row.getCell(i));
            }
            csvData += NEW_LINE_CHARACTER;
        }
        writeCSV(tmp_output, csvData);
    }

    private static String getNumericValue(XSSFCell myCell) throws Exception {
        String cellData = "";
        if (DateUtil.isCellDateFormatted(myCell)) {
            cellData += new SimpleDateFormat(OUTPUT_DATE_FORMAT).format(myCell.getDateCellValue()) + CVS_SEPERATOR_CHAR;
        }
        else {
            cellData += new BigDecimal(myCell.getNumericCellValue()).toString()+CVS_SEPERATOR_CHAR ;
        }
        return cellData;
    }

    private static String getCellData(XSSFCell myCell) throws Exception{
        String cellData = "";
        if (myCell== null) {
            cellData += CVS_SEPERATOR_CHAR;;
        }
        else {
            switch(myCell.getCellType()) {
                case STRING:
                case BOOLEAN:
                    cellData += myCell.getRichStringCellValue() + CVS_SEPERATOR_CHAR;
                    break;
                case NUMERIC:
                    cellData += getNumericValue(myCell);
                    break;
                case FORMULA:
                    break;
                default:
                    cellData += CVS_SEPERATOR_CHAR;;
            }
        }
        return cellData;
    }
    private static void writeCSV(String csvFileName,String csvData) throws Exception {
        FileOutputStream writer = new FileOutputStream(csvFileName);
        writer.write(csvData.getBytes());
        writer.close();
    }
}
