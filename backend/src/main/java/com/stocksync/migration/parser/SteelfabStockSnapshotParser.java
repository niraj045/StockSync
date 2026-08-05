package com.stocksync.migration.parser;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.migration.entity.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Component
public class SteelfabStockSnapshotParser {
    public static final String SOURCE_FORMAT="STEELFAB_STOCK_SNAPSHOT_V1";
    private static final int ITEM_NAME_COLUMN=1, FIRST_SITE_COLUMN=2, LAST_SITE_COLUMN=17, GODOWN_COLUMN=20;
    private static final Pattern DATE=Pattern.compile("(\\d{2})[./-](\\d{2})[./-](\\d{2,4})");

    public ParsedWorkbook parse(InputStream input){
        try(Workbook workbook=WorkbookFactory.create(input)){
            Sheet sheet=null;
            for(int i=0; i<workbook.getNumberOfSheets(); i++){
                Sheet s=workbook.getSheetAt(i);
                Row header=s.getRow(3);
                if(header!=null && "Party Name".equalsIgnoreCase(text(header.getCell(ITEM_NAME_COLUMN)))){
                    sheet=s;
                    break;
                }
            }
            if(sheet==null)throw invalid("Could not find a sheet with 'Party Name' in cell B4");
            Row header=sheet.getRow(3);
            LocalDate partyDate=parseDate(text(sheet.getRow(1).getCell(0)),"party snapshot");
            LocalDate godownDate=parseDate(text(header.getCell(GODOWN_COLUMN)),"godown snapshot");
            LinkedHashMap<String,String> locations=new LinkedHashMap<>();
            for(int column=FIRST_SITE_COLUMN;column<=LAST_SITE_COLUMN;column++){
                String name=rawText(header.getCell(column));
                if(name.isBlank())throw invalid("Party/site column "+columnName(column)+" has no source name");
                locations.put(columnName(column),name);
            }
            List<SourceItem> sourceItems=new ArrayList<>();
            for(int rowIndex=4;rowIndex<=sheet.getLastRowNum();rowIndex++){
                Row row=sheet.getRow(rowIndex);if(row==null)continue;
                String sr=rawText(row.getCell(0));String item=rawText(row.getCell(ITEM_NAME_COLUMN));
                if(sr.isBlank()&&item.isBlank())continue;
                if(item.isBlank())throw invalid("Source item name is missing at Excel row "+(rowIndex+1));
                sourceItems.add(new SourceItem(rowIndex+1,sr,item,normalize(item)));
            }
            if(sourceItems.size()!=42)throw invalid("Expected 42 source material rows but found "+sourceItems.size());
            Map<String,Long> nameCounts=new HashMap<>();
            sourceItems.forEach(i->nameCounts.merge(normalizeKey(i.sourceItemName()),1L,Long::sum));
            List<ParsedBalance> balances=new ArrayList<>();
            for(SourceItem item:sourceItems){
                Row row=sheet.getRow(item.sourceExcelRow()-1);
                boolean duplicate=nameCounts.get(normalizeKey(item.sourceItemName()))>1;
                for(int column=FIRST_SITE_COLUMN;column<=LAST_SITE_COLUMN;column++){
                    BigDecimal quantity=quantity(row.getCell(column),item.sourceExcelRow(),columnName(column));
                    if(quantity.signum()==0)continue;
                    balances.add(balance(item,columnName(column),locations.get(columnName(column)),quantity,partyDate,
                            ImportLocationType.PARTY_OR_SITE,TargetStockBucket.ISSUED,OpeningTransactionType.OPENING_SITE_BALANCE,
                            warning(item.sourceItemName(),duplicate)));
                }
                BigDecimal godown=quantity(row.getCell(GODOWN_COLUMN),item.sourceExcelRow(),"U");
                if(godown.signum()>0)balances.add(balance(item,"U","Godown",godown,godownDate,ImportLocationType.GODOWN,
                        TargetStockBucket.AVAILABLE,OpeningTransactionType.OPENING_GODOWN_BALANCE,warning(item.sourceItemName(),duplicate)));
            }
            BigDecimal partyTotal=balances.stream().filter(b->b.locationType()==ImportLocationType.PARTY_OR_SITE)
                    .map(ParsedBalance::quantity).reduce(BigDecimal.ZERO,BigDecimal::add);
            BigDecimal godownTotal=balances.stream().filter(b->b.locationType()==ImportLocationType.GODOWN)
                    .map(ParsedBalance::quantity).reduce(BigDecimal.ZERO,BigDecimal::add);
            List<String>warnings=new ArrayList<>();
            warnings.add("Source totals in columns T and V are ignored; StockSync recalculates from C:R and U.");
            warnings.add("Party/site stock is dated "+partyDate+" while godown stock is dated "+godownDate+".");
            if(nameCounts.values().stream().anyMatch(v->v>1))warnings.add("Duplicate source item names require explicit confirmation.");
            return new ParsedWorkbook(sourceItems,locations,balances,partyDate,godownDate,partyTotal,godownTotal,warnings);
        }catch(BusinessRuleException e){throw e;}catch(Exception e){throw new BusinessRuleException("IMPORT_PARSE_FAILED","Unable to parse workbook: "+e.getMessage());}
    }
    private ParsedBalance balance(SourceItem item,String column,String location,BigDecimal quantity,LocalDate date,
            ImportLocationType type,TargetStockBucket bucket,OpeningTransactionType transaction,String warning){
        return new ParsedBalance(item.sourceExcelRow(),column,item.sourceSrNumber(),item.sourceItemName(),item.normalizedSuggestion(),
                location,quantity,date,type,bucket,transaction,warning);
    }
    private BigDecimal quantity(Cell cell,int row,String column){
        if(cell==null||cell.getCellType()==CellType.BLANK)return BigDecimal.ZERO;
        BigDecimal value;
        if(cell.getCellType()==CellType.NUMERIC)value=BigDecimal.valueOf(cell.getNumericCellValue());
        else if(cell.getCellType()==CellType.STRING&&text(cell).isBlank())return BigDecimal.ZERO;
        else if(cell.getCellType()==CellType.STRING){try{value=new BigDecimal(text(cell).replace(",",""));}catch(NumberFormatException e){throw invalid("Invalid quantity at "+column+row);}}
        else throw invalid("Quantity at "+column+row+" must be numeric");
        value=value.stripTrailingZeros();if(value.signum()<0)throw invalid("Negative quantity at "+column+row+" is not allowed");
        return value;
    }
    private String warning(String name,boolean duplicate){
        List<String>warnings=new ArrayList<>();if(duplicate)warnings.add("Duplicate source item name; explicit confirmation required");
        String key=normalizeKey(name);if(key.equals("damage h frame"))warnings.add("Ambiguous damaged stock: create a separate item or exclude pending clarification");
        if(key.equals("out size h frame"))warnings.add("Variant decision required: create a separate item or exclude pending clarification");
        return warnings.isEmpty()?null:String.join("; ",warnings);
    }
    private LocalDate parseDate(String value,String label){
        Matcher m=DATE.matcher(value);if(!m.find())throw invalid("Could not identify "+label+" date");
        String year=m.group(3);if(year.length()==2)year="20"+year;
        return LocalDate.parse(m.group(1)+"."+m.group(2)+"."+year,DateTimeFormatter.ofPattern("dd.MM.uuuu"));
    }
    private String rawText(Cell cell){if(cell==null)return "";return new DataFormatter(Locale.ROOT).formatCellValue(cell);}
    private String text(Cell cell){return rawText(cell).trim();}
    private String normalize(String value){return value.trim().replaceAll("\\s+"," ");}
    private String normalizeKey(String value){return normalize(value).toLowerCase(Locale.ROOT);}
    private String columnName(int zeroBased){int n=zeroBased+1;StringBuilder b=new StringBuilder();while(n>0){int r=(n-1)%26;b.append((char)('A'+r));n=(n-1)/26;}return b.reverse().toString();}
    private BusinessRuleException invalid(String message){return new BusinessRuleException("INVALID_STEELFAB_WORKBOOK",message);}
    public record SourceItem(int sourceExcelRow,String sourceSrNumber,String sourceItemName,String normalizedSuggestion){}
    public record ParsedBalance(int sourceExcelRow,String sourceExcelColumn,String sourceSrNumber,String sourceItemName,
            String normalizedSuggestion,String sourceLocationName,BigDecimal quantity,LocalDate snapshotDate,
            ImportLocationType locationType,TargetStockBucket targetStockBucket,OpeningTransactionType openingTransactionType,String warning){}
    public record ParsedWorkbook(List<SourceItem>sourceItems,Map<String,String>locations,List<ParsedBalance>balances,
            LocalDate partySnapshotDate,LocalDate godownSnapshotDate,BigDecimal partyTotal,BigDecimal godownTotal,List<String>warnings){
        public BigDecimal combinedTotal(){return partyTotal.add(godownTotal);}
    }
}
