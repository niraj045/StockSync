package com.stocksync.reporting.service;

import com.stocksync.common.exception.BusinessRuleException;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SbutWorkbookService {
    private static final int FIRST_DATA_ROW=9;
    private static final int TEMPLATE_TOTAL_ROW=22;
    private final JdbcTemplate jdbc;
    public SbutWorkbookService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Transactional(readOnly=true)
    public WorkbookDownload generate(long siteId){
        SiteInfo site=jdbc.query("SELECT site_name,excel_template_code FROM sites WHERE id=?",rs->{if(!rs.next())throw new BusinessRuleException("SITE_NOT_FOUND","Site not found");return new SiteInfo(rs.getString(1),rs.getString(2));},siteId);
        if(!"SBUT_DR_V1".equals(site.templateCode()))throw new BusinessRuleException("SBUT_TEMPLATE_NOT_ASSIGNED","Assign SBUT D&R format to this site before exporting");
        LinkedHashMap<String,Movement> movements=new LinkedHashMap<>();
        jdbc.query("""
            SELECT ic.challan_number document_number,ic.dispatch_date movement_date,ici.item_name_snapshot item_name,ici.quantity quantity
            FROM issued_challans ic JOIN site_orders so ON so.id=ic.site_order_id JOIN issued_challan_items ici ON ici.issued_challan_id=ic.id
            WHERE so.site_id=? ORDER BY ic.dispatch_date,ic.id,ici.id
            """,rs->{add(movements,rs.getString(1),rs.getObject(2,LocalDate.class),rs.getString(3),rs.getBigDecimal(4));},siteId);
        jdbc.query("""
            SELECT CONCAT('R-',rc.receiving_challan_number),rc.receive_date,rci.item_name_snapshot,
                   -(rci.good_returned_quantity+rci.damaged_returned_quantity+rci.extra_returned_quantity)
            FROM receiving_challans rc JOIN receiving_challan_items rci ON rci.receiving_challan_id=rc.id
            WHERE rc.site_id=? AND rc.status='POSTED' ORDER BY rc.receive_date,rc.id,rci.id
            """,rs->{add(movements,rs.getString(1),rs.getObject(2,LocalDate.class),rs.getString(3),rs.getBigDecimal(4));},siteId);
        try(InputStream in=new ClassPathResource("excel-templates/sbut-dr-v1.xlsx").getInputStream();XSSFWorkbook workbook=new XSSFWorkbook(in);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            Sheet sheet=workbook.getSheetAt(0);Row sample=sheet.getRow(FIRST_DATA_ROW);int required=Math.max(13,movements.size());int extra=required-13;
            if(extra>0)sheet.shiftRows(TEMPLATE_TOTAL_ROW,sheet.getLastRowNum(),extra,true,false);
            for(int index=0;index<required;index++){
                int rowIndex=FIRST_DATA_ROW+index;Row row=sheet.getRow(rowIndex);if(row==null)row=sheet.createRow(rowIndex);
                copyStyle(sample,row);clear(row,1,9);
            }
            int index=0;for(Movement movement:movements.values()){
                Row row=sheet.getRow(FIRST_DATA_ROW+index);row.getCell(1).setCellValue(index+1);row.getCell(2).setCellValue(movement.document());row.getCell(3).setCellValue(movement.date());
                for(var entry:movement.quantities().entrySet()){Integer column=column(entry.getKey());if(column!=null)row.getCell(column).setCellValue(entry.getValue().doubleValue());}
                index++;
            }
            int totalRowIndex=FIRST_DATA_ROW+required;Row total=sheet.getRow(totalRowIndex);if(total==null)total=sheet.createRow(totalRowIndex);
            total.getCell(1,Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue("Total");
            for(int column=4;column<=9;column++){String letter=org.apache.poi.ss.util.CellReference.convertNumToColString(column);total.getCell(column,Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellFormula("SUM("+letter+(FIRST_DATA_ROW+1)+":"+letter+(FIRST_DATA_ROW+required)+")");}
            Row siteRow=sheet.getRow(7);if(siteRow!=null)siteRow.getCell(1,Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(site.name());
            workbook.setForceFormulaRecalculation(true);workbook.write(out);
            return new WorkbookDownload(out.toByteArray(),"SBUT-D-R-"+LocalDate.now()+".xlsx");
        }catch(IOException ex){throw new BusinessRuleException("SBUT_EXPORT_FAILED","Unable to generate SBUT workbook");}
    }
    private void add(Map<String,Movement> rows,String document,LocalDate date,String item,BigDecimal quantity){Movement row=rows.computeIfAbsent(document,k->new Movement(document,date,new LinkedHashMap<>()));String key=normalize(item);row.quantities().merge(key,quantity,BigDecimal::add);}
    private Integer column(String item){if(item.contains("hframe")||item.contains("hframes"))return 4;if(item.contains("bracing"))return 5;if(item.contains("platform"))return 6;if(item.contains("platepipe")||item.contains("pipe"))return 7;if(item.contains("basejack"))return 8;if(item.contains("coupler"))return 9;return null;}
    private String normalize(String v){return v==null?"":v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private void copyStyle(Row source,Row target){target.setHeight(source.getHeight());for(int c=1;c<=9;c++){Cell from=source.getCell(c,Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);Cell to=target.getCell(c,Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);to.setCellStyle(from.getCellStyle());}}
    private void clear(Row row,int from,int to){for(int c=from;c<=to;c++)row.getCell(c,Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setBlank();}
    private record SiteInfo(String name,String templateCode){}private record Movement(String document,LocalDate date,Map<String,BigDecimal> quantities){}public record WorkbookDownload(byte[] bytes,String filename){}
}
