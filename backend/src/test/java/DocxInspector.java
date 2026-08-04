import java.io.FileInputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import java.util.List;

public class DocxInspector {
    public static void main(String[] args) throws Exception {
        String path = args[0];
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            
            List<XWPFTable> tables = doc.getTables();
            System.out.println("Total tables: " + tables.size());
            for (int i = 0; i < tables.size(); i++) {
                XWPFTable t = tables.get(i);
                System.out.println("Table " + i + " has " + t.getRows().size() + " rows");
                for (int r = 0; r < t.getRows().size(); r++) {
                    XWPFTableRow row = t.getRow(r);
                    System.out.println("  Row " + r + " has " + row.getTableCells().size() + " cells");
                    for (int c = 0; c < row.getTableCells().size(); c++) {
                        XWPFTableCell cell = row.getCell(c);
                        System.out.println("    Cell " + c + " text: " + cell.getText().replace("\n", "\\n").substring(0, Math.min(30, cell.getText().length())));
                    }
                }
            }
        }
    }
}
