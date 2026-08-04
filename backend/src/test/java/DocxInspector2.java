import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream;

public class DocxInspector2 {
    public static void main(String[] args) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new FileInputStream("backend/src/main/resources/pdf-templates/steelfab_delivery_challan_exact_editable.docx"))) {
            int tIdx = 0;
            for (XWPFTable t : doc.getTables()) {
                System.out.println("Table " + tIdx++);
                for (int rIdx = 0; rIdx < t.getRows().size(); rIdx++) {
                    XWPFTableRow r = t.getRow(rIdx);
                    for (int cIdx = 0; cIdx < r.getTableCells().size(); cIdx++) {
                        String txt = r.getCell(cIdx).getText();
                        if (txt != null && !txt.isBlank()) {
                            System.out.println("  R" + rIdx + "C" + cIdx + ": " + txt);
                        }
                    }
                }
            }
        }
    }
}
