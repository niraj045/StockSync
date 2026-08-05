import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
public class ExcelDumper {
    public static void main(String[] args) throws Exception {
        File file = new File("/home/ainosoft/NIraj-workspace/StockSync/client-data/SBUT D&R.xlsx");
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0); // D & R
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                System.out.print("Row " + r + ": ");
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        continue;
                    }
                    if (cell.getCellType() == CellType.STRING) {
                        System.out.print("[" + cell.getStringCellValue() + "] ");
                    } else if (cell.getCellType() == CellType.NUMERIC) {
                        System.out.print("[" + cell.getNumericCellValue() + "] ");
                    }
                }
                System.out.println();
            }
        }
    }
}
