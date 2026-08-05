import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;

public class ExcelDumper {
    public static void main(String[] args) throws Exception {
        File file = new File("/home/ainosoft/NIraj-workspace/StockSync/client-data/SBUT D&R.xlsx");
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                System.out.println("Sheet: " + sheet.getSheetName());
                for (int r = 0; r < Math.min(25, sheet.getPhysicalNumberOfRows()); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;
                    System.out.print("Row " + r + ": ");
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell == null) {
                            System.out.print("[EMPTY]\t");
                            continue;
                        }
                        switch (cell.getCellType()) {
                            case STRING:
                                System.out.print("[" + cell.getStringCellValue() + "]\t");
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    System.out.print("[" + cell.getDateCellValue() + "]\t");
                                } else {
                                    System.out.print("[" + cell.getNumericCellValue() + "]\t");
                                }
                                break;
                            default:
                                System.out.print("[" + cell.getCellType() + "]\t");
                        }
                    }
                    System.out.println();
                }
            }
        }
    }
}
