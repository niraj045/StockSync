package com.stocksync.migration.service;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.service.*;
import com.stocksync.migration.dto.*;
import com.stocksync.migration.entity.*;
import com.stocksync.migration.parser.SteelfabStockSnapshotParser;
import com.stocksync.migration.repository.*;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import com.stocksync.site.service.ImportLocationAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;import java.math.BigDecimal;import java.nio.file.*;import java.security.MessageDigest;
import java.time.*;import java.util.*;import java.util.stream.Collectors;

@Service
public class StockImportService {
    private static final String SOURCE_SYSTEM=SteelfabStockSnapshotParser.SOURCE_FORMAT;
    private final StockImportBatchRepository batches;private final StockImportRowRepository rows;
    private final StockImportLocationMappingRepository locationMappings;private final ItemAliasRepository aliases;
    private final SteelfabStockSnapshotParser parser;private final ImportItemAccess itemAccess;private final ImportLocationAccess locationAccess;
    private final OpeningStockAccess stock;private final UserRepository users;private final UserActivityLogService audit;
    private final EntityManager entityManager;private final Path storageRoot;
    public StockImportService(StockImportBatchRepository batches,StockImportRowRepository rows,
            StockImportLocationMappingRepository locationMappings,ItemAliasRepository aliases,SteelfabStockSnapshotParser parser,
            ImportItemAccess itemAccess,ImportLocationAccess locationAccess,OpeningStockAccess stock,UserRepository users,
            UserActivityLogService audit,EntityManager entityManager,@Value("${stocksync.file-storage-path}")String storageRoot){
        this.batches=batches;this.rows=rows;this.locationMappings=locationMappings;this.aliases=aliases;this.parser=parser;
        this.itemAccess=itemAccess;this.locationAccess=locationAccess;this.stock=stock;this.users=users;this.audit=audit;
        this.entityManager=entityManager;this.storageRoot=Paths.get(storageRoot).toAbsolutePath().normalize();}

    @Transactional
    public StockImportBatchResponse upload(MultipartFile file,String notes,HttpServletRequest http){
        if(file==null||file.isEmpty())throw rule("FILE_EMPTY","Select an XLSX workbook");
        if(file.getSize()>20L*1024*1024)throw rule("FILE_TOO_LARGE","Opening-stock workbook must not exceed 20 MB");
        String original=Paths.get(Optional.ofNullable(file.getOriginalFilename()).orElse("opening-stock.xlsx")).getFileName().toString();
        if(!original.toLowerCase(Locale.ROOT).endsWith(".xlsx"))throw rule("FILE_TYPE_NOT_ALLOWED","Only XLSX workbooks are supported");
        byte[] content;try{content=file.getBytes();}catch(IOException e){throw rule("FILE_READ_FAILED","Unable to read uploaded workbook");}
        String checksum=sha256(content);if(batches.existsByFileChecksum(checksum))throw rule("IMPORT_FILE_ALREADY_EXISTS","This workbook has already been uploaded");
        var parsed=parser.parse(new ByteArrayInputStream(content));String code="OSI-"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
        String stored=UUID.randomUUID()+".xlsx";Path directory=storageRoot.resolve("stock-imports").resolve(code).normalize();
        Path destination=directory.resolve(stored).normalize();if(!destination.startsWith(storageRoot))throw rule("INVALID_FILE_PATH","Invalid import file path");
        try{Files.createDirectories(directory);Files.write(destination,content,StandardOpenOption.CREATE_NEW);}catch(IOException e){throw rule("FILE_STORAGE_FAILED","Unable to store import workbook");}
        try{
            StockImportBatch batch=new StockImportBatch();batch.setBatchCode(code);batch.setOriginalFilename(original);batch.setStoredFilename(stored);
            batch.setStoragePath(storageRoot.relativize(destination).toString());batch.setFileChecksum(checksum);
            batch.setPartySnapshotDate(parsed.partySnapshotDate());batch.setGodownSnapshotDate(parsed.godownSnapshotDate());
            batch.setStatus(ImportBatchStatus.PARSED);batch.setTotalSourceRows(parsed.sourceItems().size());batch.setTotalBalanceRows(parsed.balances().size());
            batch.setExpectedPartyTotal(parsed.partyTotal());batch.setExpectedGodownTotal(parsed.godownTotal());batch.setNotes(trim(notes));
            batch.setCreatedBy(actor());batch.setUpdatedBy(actor());batch=batches.save(batch);
            for(var parsedRow:parsed.balances())rows.save(toEntity(batch,parsedRow));
            batch.setStatus(ImportBatchStatus.MAPPING_REQUIRED);refreshCounts(batch);batch=batches.save(batch);
            log("STOCK_IMPORT_UPLOADED",batch,original,http);log("STOCK_IMPORT_PARSED",batch,
                parsed.sourceItems().size()+" source rows, "+parsed.balances().size()+" balance rows",http);
            return response(batch);
        }catch(RuntimeException e){try{Files.deleteIfExists(destination);}catch(IOException ignored){}throw e;}
    }
    @Transactional(readOnly=true)
    public Page<StockImportBatchResponse> list(String search,ImportBatchStatus status,Pageable pageable){
        return batches.findAll((root,q,cb)->{List<Predicate>p=new ArrayList<>();if(search!=null&&!search.isBlank()){
            String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";p.add(cb.or(cb.like(cb.lower(root.get("batchCode")),t),
                cb.like(cb.lower(root.get("originalFilename")),t),cb.like(cb.lower(root.get("fileChecksum")),t)));}
            if(status!=null)p.add(cb.equal(root.get("status"),status));return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::response);}
    @Transactional(readOnly=true)public StockImportBatchResponse get(Long id){return response(batch(id));}
    @Transactional(readOnly=true)public Page<StockImportRowResponse> rowPage(Long batchId,ImportValidationStatus status,
            ImportLocationType locationType,String search,Pageable pageable){
        batch(batchId);return rows.findAll((root,q,cb)->{List<Predicate>p=new ArrayList<>();p.add(cb.equal(root.get("batch").get("id"),batchId));
            if(status!=null)p.add(cb.equal(root.get("validationStatus"),status));if(locationType!=null)p.add(cb.equal(root.get("locationType"),locationType));
            if(search!=null&&!search.isBlank()){String t="%"+search.trim().toLowerCase(Locale.ROOT)+"%";p.add(cb.or(
                cb.like(cb.lower(root.get("sourceItemName")),t),cb.like(cb.lower(root.get("sourceLocationName")),t),
                cb.like(cb.lower(root.get("sourceExcelColumn")),t)));}return cb.and(p.toArray(Predicate[]::new));},pageable).map(this::rowResponse);}

    @Transactional
    public StockImportBatchResponse mapItem(Long batchId,Long rowId,ItemMappingRequest request,HttpServletRequest http){
        StockImportBatch batch=editable(batchId);StockImportRow selected=rows.findByIdAndBatchId(rowId,batchId)
            .orElseThrow(()->rule("IMPORT_ROW_NOT_FOUND","Import row not found"));
        if(selected.getVersion()!=request.version())throw new ObjectOptimisticLockingFailureException(StockImportRow.class,rowId);
        List<StockImportRow> sourceRows=rows.findByBatchIdAndSourceExcelRow(batchId,selected.getSourceExcelRow());
        if(request.exclude()){
            if(request.exclusionReason()==null||request.exclusionReason().isBlank())throw rule("EXCLUSION_REASON_REQUIRED","Explain why this source item is excluded");
            sourceRows.forEach(r->{r.setExcluded(true);r.setExclusionReason(request.exclusionReason().trim());r.setMappedItem(null);
                r.setDuplicateConfirmed(request.confirmDuplicate());r.setValidationStatus(ImportValidationStatus.WARNING);
                r.setValidationMessage("Excluded: "+request.exclusionReason().trim());});rows.saveAll(sourceRows);
        }else{
            ImportItemAccess.ItemView item=request.createNew()?itemAccess.create(defaultCode(request,selected),defaultName(request,selected),actor())
                :itemAccess.byId(request.itemId()).filter(ImportItemAccess.ItemView::active)
                    .orElseThrow(()->rule("ITEM_NOT_FOUND_OR_INACTIVE","Select an active item"));
            Item reference=entityManager.getReference(Item.class,item.id());
            sourceRows.forEach(r->{r.setExcluded(false);r.setExclusionReason(null);r.setMappedItem(reference);
                r.setDuplicateConfirmed(request.confirmDuplicate());evaluate(r);});rows.saveAll(sourceRows);
            if(request.saveAlias()&&!aliases.existsByAliasIgnoreCaseAndSourceSystem(selected.getSourceItemName(),SOURCE_SYSTEM)){
                ItemAlias alias=new ItemAlias();alias.setItem(reference);alias.setAlias(selected.getSourceItemName());alias.setSourceSystem(SOURCE_SYSTEM);aliases.save(alias);}
        }
        batch.setStatus(ImportBatchStatus.MAPPING_REQUIRED);refreshCounts(batch);batches.save(batch);
        log("STOCK_IMPORT_MAPPING_UPDATED",batch,"Item mapping for source Excel row "+selected.getSourceExcelRow(),http);return response(batch);
    }
    @Transactional
    public List<LocationMappingResponse> mapLocation(Long batchId,LocationMappingRequest request,HttpServletRequest http){
        StockImportBatch batch=editable(batchId);String column=request.sourceExcelColumn().trim().toUpperCase(Locale.ROOT);
        List<StockImportRow> columnRows=rows.findByBatchIdAndSourceExcelColumn(batchId,column);
        if(columnRows.isEmpty()||columnRows.getFirst().getLocationType()!=ImportLocationType.PARTY_OR_SITE)
            throw rule("SOURCE_LOCATION_NOT_FOUND","Party/site source column not found");
        var location=locationAccess.resolve(request.partyId(),request.siteId(),request.createParty(),request.partyName(),
            request.createSite(),request.siteName(),request.siteCode(),actor());
        Party party=entityManager.getReference(Party.class,location.partyId());Site site=entityManager.getReference(Site.class,location.siteId());
        StockImportLocationMapping mapping=locationMappings.findByBatchIdAndSourceExcelColumn(batchId,column).orElseGet(StockImportLocationMapping::new);
        if(mapping.getId()==null){mapping.setBatch(batch);mapping.setSourceExcelColumn(column);mapping.setSourceLocationName(columnRows.getFirst().getSourceLocationName());
            mapping.setCreatedBy(actor());}mapping.setMappedParty(party);mapping.setMappedSite(site);mapping.setUpdatedBy(actor());locationMappings.save(mapping);
        columnRows.forEach(r->{r.setMappedParty(party);r.setMappedSite(site);evaluate(r);});rows.saveAll(columnRows);
        batch.setStatus(ImportBatchStatus.MAPPING_REQUIRED);refreshCounts(batch);batches.save(batch);
        log("STOCK_IMPORT_MAPPING_UPDATED",batch,"Location mapping for "+columnRows.getFirst().getSourceLocationName(),http);return locations(batchId);
    }
    @Transactional(readOnly=true)
    public List<LocationMappingResponse> locations(Long batchId){
        batch(batchId);Map<String,StockImportLocationMapping>mapped=locationMappings.findByBatchIdOrderBySourceExcelColumn(batchId).stream()
            .collect(Collectors.toMap(StockImportLocationMapping::getSourceExcelColumn,m->m));
        return rows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(batchId).stream()
            .filter(r->r.getLocationType()==ImportLocationType.PARTY_OR_SITE).collect(Collectors.toMap(StockImportRow::getSourceExcelColumn,
                StockImportRow::getSourceLocationName,(a,b)->a,TreeMap::new)).entrySet().stream().map(e->{var m=mapped.get(e.getKey());
                return m==null?new LocationMappingResponse(e.getKey(),e.getValue(),null,null,null,null,false):
                    new LocationMappingResponse(e.getKey(),e.getValue(),m.getMappedParty().getId(),m.getMappedParty().getLegalName(),
                        m.getMappedSite().getId(),m.getMappedSite().getSiteName(),true);}).toList();}

    @Transactional
    public StockImportPreviewResponse validate(Long id,HttpServletRequest http){
        StockImportBatch batch=editable(id);List<StockImportRow> all=rows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(id);
        all.forEach(this::evaluate);rows.saveAll(all);refreshCounts(batch);
        boolean unresolved=all.stream().anyMatch(r->!r.isExcluded()&&(r.getValidationStatus()==ImportValidationStatus.MAPPING_REQUIRED||r.getValidationStatus()==ImportValidationStatus.ERROR));
        batch.setStatus(unresolved?ImportBatchStatus.MAPPING_REQUIRED:ImportBatchStatus.VALIDATED);batches.save(batch);
        StockImportPreviewResponse preview=preview(batch,all);if(unresolved)return preview;
        if(!preview.postable())throw rule("IMPORT_TOTALS_UNEXPLAINED","Mapped and excluded totals do not reconcile to source totals");
        log("STOCK_IMPORT_VALIDATED",batch,"Corrected combined total "+preview.expectedCombinedTotal(),http);return preview;
    }
    @Transactional(readOnly=true)public StockImportPreviewResponse preview(Long id){StockImportBatch batch=batch(id);
        return preview(batch,rows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(id));}
    @Transactional
    public StockImportPreviewResponse post(Long id,PostImportRequest request,HttpServletRequest http){
        StockImportBatch batch=batchForUpdate(id);if(batch.getStatus()==ImportBatchStatus.POSTED)throw rule("IMPORT_ALREADY_POSTED","Import batch is already posted");
        if(batch.getStatus()!=ImportBatchStatus.VALIDATED)throw rule("IMPORT_NOT_VALIDATED","Validate the import before posting");
        if(!batch.getFileChecksum().equalsIgnoreCase(request.expectedChecksum()))throw rule("IMPORT_CHECKSUM_MISMATCH","Posting confirmation checksum does not match");
        List<StockImportRow> all=rows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(id);StockImportPreviewResponse before=preview(batch,all);
        if(!before.postable())throw rule("IMPORT_TOTALS_UNEXPLAINED","Preview totals do not reconcile to the source");
        try{
            for(StockImportRow row:postingOrder(all)){if(row.isExcluded())continue;if(row.getPostedStockTransaction()!=null)throw rule("IMPORT_ROW_ALREADY_POSTED","An import row is already posted");
                Long txId=stock.post(new OpeningStockAccess.OpeningCommand(batch.getId(),row.getId(),row.getMappedItem().getId(),
                    row.getMappedParty()==null?null:row.getMappedParty().getId(),row.getMappedSite()==null?null:row.getMappedSite().getId(),
                    row.getOpeningTransactionType().name(),row.getTargetStockBucket().name(),row.getSnapshotDate(),row.getQuantity(),
                    "Opening stock from "+batch.getBatchCode()+" cell "+row.getSourceExcelColumn()+row.getSourceExcelRow(),actor()));
                row.setPostedStockTransaction(entityManager.getReference(com.stocksync.inventory.entity.StockTransaction.class,txId));
                row.setValidationStatus(ImportValidationStatus.POSTED);}
            rows.saveAll(all);batch.setStatus(ImportBatchStatus.POSTED);batch.setImportedAt(Instant.now());batch.setImportedBy(actor());
            refreshCounts(batch);batches.save(batch);log("STOCK_IMPORT_POSTED",batch,"Posted "+all.stream().filter(r->!r.isExcluded()).count()+" opening rows",http);
            return preview(batch,all);
        }catch(RuntimeException ex){log("STOCK_IMPORT_FAILED",batch,ex.getMessage()==null?"Posting failed":safe(ex.getMessage()),http);throw ex;}
    }
    @Transactional
    public StockImportPreviewResponse reverse(Long id,ReverseImportRequest request,HttpServletRequest http){
        StockImportBatch batch=batchForUpdate(id);if(batch.getStatus()==ImportBatchStatus.REVERSED)throw rule("IMPORT_ALREADY_REVERSED","Import batch is already reversed");
        if(batch.getStatus()!=ImportBatchStatus.POSTED)throw rule("IMPORT_NOT_POSTED","Only a posted import can be reversed");
        List<StockImportRow> all=rows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(id);
        Set<Long>itemIds=all.stream().filter(r->!r.isExcluded()).map(r->r.getMappedItem().getId()).collect(Collectors.toSet());
        stock.assertReversalSafe(id,batch.getImportedAt(),itemIds);
        for(StockImportRow row:postingOrder(all)){if(row.isExcluded())continue;if(row.getPostedStockTransaction()==null)throw rule("IMPORT_TRANSACTION_NOT_FOUND","Posted row has no ledger reference");
            stock.reverse(new OpeningStockAccess.ReversalCommand(batch.getId(),row.getId(),row.getPostedStockTransaction().getId(),
                row.getMappedItem().getId(),row.getMappedParty()==null?null:row.getMappedParty().getId(),row.getMappedSite()==null?null:row.getMappedSite().getId(),
                row.getTargetStockBucket().name(),LocalDate.now(),row.getQuantity(),request.reason().trim(),actor()));
            row.setValidationStatus(ImportValidationStatus.REVERSED);}
        rows.saveAll(all);batch.setStatus(ImportBatchStatus.REVERSED);batch.setReversedAt(Instant.now());batch.setReversedBy(actor());
        batch.setReversalReason(request.reason().trim());refreshCounts(batch);batches.save(batch);
        log("STOCK_IMPORT_REVERSED",batch,request.reason().trim(),http);return preview(batch,all);
    }
    @Transactional(readOnly=true)public StockImportReportResponse report(Long id){StockImportBatch batch=batch(id);
        return new StockImportReportResponse(response(batch),preview(batch,rows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(id)),locations(id),
            List.of("Source columns T and V are not authoritative because T omits P:R.","P:R contributes 2,418 units omitted by the source formula.",
                "Party/site stock date and godown stock date differ.","Duplicate item name '8ft & 10ft plate pipe' occurs at source Sr.No. 33 and 35."),
            List.of("Confirm whether each source party column is a legal party, site, or project.",
                "Decide whether 'Damage H frame' is a separate item or should remain excluded.","Decide whether 'Out size H frame' is a separate variant.",
                "Confirm normalized spellings and shorthand names before creating new items."));}

    private StockImportRow toEntity(StockImportBatch batch,SteelfabStockSnapshotParser.ParsedBalance p){
        StockImportRow row=new StockImportRow();row.setBatch(batch);row.setSourceExcelRow(p.sourceExcelRow());row.setSourceExcelColumn(p.sourceExcelColumn());
        row.setSourceSrNumber(p.sourceSrNumber());row.setSourceItemName(p.sourceItemName());row.setNormalizedItemSuggestion(p.normalizedSuggestion());
        row.setSourceLocationName(p.sourceLocationName());row.setLocationType(p.locationType());row.setQuantity(p.quantity());row.setSnapshotDate(p.snapshotDate());
        row.setTargetStockBucket(p.targetStockBucket());row.setOpeningTransactionType(p.openingTransactionType());row.setMappedGodownCode(p.locationType()==ImportLocationType.GODOWN?"MAIN":null);
        boolean ambiguous=p.warning()!=null&&(p.warning().contains("Duplicate")||p.warning().contains("decision")||p.warning().contains("Ambiguous"));
        if(!ambiguous){Optional<ItemAlias>alias=aliases.findByAliasIgnoreCaseAndSourceSystemAndActiveTrue(p.sourceItemName(),SOURCE_SYSTEM);
            Optional<ImportItemAccess.ItemView>match=alias.map(a->new ImportItemAccess.ItemView(a.getItem().getId(),a.getItem().getItemCode(),a.getItem().getItemName(),a.getItem().isActive()));
            if(match.isEmpty())match=itemAccess.byExactName(p.sourceItemName());match.filter(ImportItemAccess.ItemView::active)
                .ifPresent(i->row.setMappedItem(entityManager.getReference(Item.class,i.id())));}
        row.setValidationMessage(p.warning());evaluate(row);return row;
    }
    private void evaluate(StockImportRow row){
        if(row.isExcluded()){if(row.getExclusionReason()==null||row.getExclusionReason().isBlank()){row.setValidationStatus(ImportValidationStatus.ERROR);
            row.setValidationMessage("Excluded row requires a reason");}else{row.setValidationStatus(ImportValidationStatus.WARNING);row.setValidationMessage("Excluded: "+row.getExclusionReason());}return;}
        List<String>issues=new ArrayList<>();if(row.getMappedItem()==null)issues.add("Item mapping required");
        if(row.getLocationType()==ImportLocationType.PARTY_OR_SITE&&(row.getMappedParty()==null||row.getMappedSite()==null))issues.add("Party and site mapping required");
        String ambiguity=ambiguityMessage(row.getSourceItemName());boolean ambiguous=ambiguity!=null;
        if(ambiguous&&!row.isDuplicateConfirmed())issues.add(ambiguity+" Explicit confirmation is required");
        if(!issues.isEmpty()){row.setValidationStatus(ImportValidationStatus.MAPPING_REQUIRED);row.setValidationMessage(String.join("; ",issues));}
        else{row.setValidationStatus(ambiguous?ImportValidationStatus.WARNING:ImportValidationStatus.READY);
            row.setValidationMessage(ambiguous?ambiguity+" Decision explicitly confirmed.":null);}
    }
    private boolean isAmbiguous(String name){String key=name.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
        return key.equals("8ft & 10ft plate pipe")||key.equals("damage h frame")||key.equals("out size h frame");}
    private String ambiguityMessage(String name){String key=name.trim().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);
        return switch(key){case "8ft & 10ft plate pipe"->"Duplicate source name occurs at Sr.No. 33 and 35; do not merge automatically.";
            case "damage h frame"->"Damage H frame may be a stock condition; create a separate item or exclude pending clarification.";
            case "out size h frame"->"Out size H frame requires an explicit separate-item or variant decision.";default->null;};}
    private void refreshCounts(StockImportBatch batch){List<StockImportRow>all=rows.findByBatchIdOrderBySourceExcelRowAscSourceExcelColumnAsc(batch.getId());
        batch.setValidRows((int)all.stream().filter(r->EnumSet.of(ImportValidationStatus.READY,ImportValidationStatus.WARNING,
            ImportValidationStatus.POSTED,ImportValidationStatus.REVERSED).contains(r.getValidationStatus())).count());
        batch.setWarningRows((int)all.stream().filter(r->r.getValidationStatus()==ImportValidationStatus.WARNING).count());
        batch.setErrorRows((int)all.stream().filter(r->r.getValidationStatus()==ImportValidationStatus.ERROR||r.getValidationStatus()==ImportValidationStatus.MAPPING_REQUIRED).count());}
    private StockImportPreviewResponse preview(StockImportBatch batch,List<StockImportRow>all){
        java.util.function.Predicate<StockImportRow>resolved=r->!r.isExcluded()&&EnumSet.of(ImportValidationStatus.READY,
            ImportValidationStatus.WARNING,ImportValidationStatus.POSTED,ImportValidationStatus.REVERSED).contains(r.getValidationStatus());
        BigDecimal mappedParty=sum(all,r->resolved.test(r)&&r.getLocationType()==ImportLocationType.PARTY_OR_SITE);
        BigDecimal mappedGodown=sum(all,r->resolved.test(r)&&r.getLocationType()==ImportLocationType.GODOWN);
        BigDecimal excludedParty=sum(all,r->r.isExcluded()&&r.getLocationType()==ImportLocationType.PARTY_OR_SITE);
        BigDecimal excludedGodown=sum(all,r->r.isExcluded()&&r.getLocationType()==ImportLocationType.GODOWN);
        BigDecimal excluded=excludedParty.add(excludedGodown);
        BigDecimal errorParty=sum(all,r->!r.isExcluded()&&r.getLocationType()==ImportLocationType.PARTY_OR_SITE&&
            (r.getValidationStatus()==ImportValidationStatus.ERROR||r.getValidationStatus()==ImportValidationStatus.MAPPING_REQUIRED));
        BigDecimal errorGodown=sum(all,r->!r.isExcluded()&&r.getLocationType()==ImportLocationType.GODOWN&&
            (r.getValidationStatus()==ImportValidationStatus.ERROR||r.getValidationStatus()==ImportValidationStatus.MAPPING_REQUIRED));
        BigDecimal errors=errorParty.add(errorGodown);
        BigDecimal postedParty=sum(all,r->r.getValidationStatus()==ImportValidationStatus.POSTED&&r.getLocationType()==ImportLocationType.PARTY_OR_SITE);
        BigDecimal postedGodown=sum(all,r->r.getValidationStatus()==ImportValidationStatus.POSTED&&r.getLocationType()==ImportLocationType.GODOWN);
        long mapped=all.stream().filter(resolved).count();
        long warnings=all.stream().filter(r->r.getValidationStatus()==ImportValidationStatus.WARNING).count();
        long errorCount=all.stream().filter(r->r.getValidationStatus()==ImportValidationStatus.ERROR||r.getValidationStatus()==ImportValidationStatus.MAPPING_REQUIRED).count();
        long excludedCount=all.stream().filter(StockImportRow::isExcluded).count();long posted=all.stream().filter(r->r.getValidationStatus()==ImportValidationStatus.POSTED).count();
        boolean partyExplained=mappedParty.add(excludedParty).add(errorParty).compareTo(batch.getExpectedPartyTotal())==0;
        boolean godownExplained=mappedGodown.add(excludedGodown).add(errorGodown).compareTo(batch.getExpectedGodownTotal())==0;
        boolean explained=partyExplained&&godownExplained;
        boolean postable=batch.getStatus()==ImportBatchStatus.VALIDATED&&errorCount==0&&explained;
        return new StockImportPreviewResponse(batch.getId(),batch.getBatchCode(),batch.getStatus().name(),batch.getPartySnapshotDate(),batch.getGodownSnapshotDate(),
            batch.getTotalSourceRows(),batch.getTotalBalanceRows(),mapped,warnings,errorCount,excludedCount,posted,batch.getExpectedPartyTotal(),batch.getExpectedGodownTotal(),
            batch.getExpectedPartyTotal().add(batch.getExpectedGodownTotal()),mappedParty,mappedGodown,mappedParty.add(mappedGodown),
            excludedParty,excludedGodown,excluded,errorParty,errorGodown,errors,postedParty,postedGodown,postedParty.add(postedGodown),
            postable,List.of("Party/site stock is dated 25-07-2026; godown stock is dated 16-07-2026.",
                "The combined total is not a same-date physical stock count.","Source total columns T and V are ignored; C:R and U are recalculated."));}
    private BigDecimal sum(List<StockImportRow>all,java.util.function.Predicate<StockImportRow>predicate){return all.stream().filter(predicate)
        .map(StockImportRow::getQuantity).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private List<StockImportRow>postingOrder(List<StockImportRow>all){return all.stream().sorted(Comparator
        .comparing((StockImportRow r)->r.getMappedItem()==null?Long.MAX_VALUE:r.getMappedItem().getId())
        .thenComparingInt(StockImportRow::getSourceExcelRow).thenComparing(StockImportRow::getSourceExcelColumn)).toList();}
    private StockImportBatch editable(Long id){StockImportBatch b=batch(id);if(EnumSet.of(ImportBatchStatus.POSTED,ImportBatchStatus.REVERSED).contains(b.getStatus()))
        throw rule("IMPORT_IMMUTABLE","Posted or reversed imports cannot be remapped");return b;}
    private StockImportBatch batch(Long id){return batches.findById(id).orElseThrow(()->rule("IMPORT_BATCH_NOT_FOUND","Import batch not found"));}
    private StockImportBatch batchForUpdate(Long id){return batches.findForUpdate(id)
        .orElseThrow(()->rule("IMPORT_BATCH_NOT_FOUND","Import batch not found"));}
    private StockImportBatchResponse response(StockImportBatch b){return new StockImportBatchResponse(b.getId(),b.getBatchCode(),b.getImportType(),b.getOriginalFilename(),
        b.getFileChecksum(),b.getSourceFormat(),b.getPartySnapshotDate(),b.getGodownSnapshotDate(),b.getStatus(),b.getTotalSourceRows(),b.getTotalBalanceRows(),
        b.getValidRows(),b.getWarningRows(),b.getErrorRows(),b.getExpectedPartyTotal(),b.getExpectedGodownTotal(),b.getExpectedPartyTotal().add(b.getExpectedGodownTotal()),
        b.getImportedAt(),b.getImportedBy(),b.getReversedAt(),b.getReversedBy(),b.getReversalReason(),b.getNotes(),b.getVersion(),b.getCreatedAt(),b.getCreatedBy());}
    private StockImportRowResponse rowResponse(StockImportRow r){Item i=r.getMappedItem();Party p=r.getMappedParty();Site s=r.getMappedSite();
        return new StockImportRowResponse(r.getId(),r.getSourceExcelRow(),r.getSourceExcelColumn(),r.getSourceSrNumber(),r.getSourceItemName(),
            r.getNormalizedItemSuggestion(),i==null?null:i.getId(),i==null?null:i.getItemCode(),i==null?null:i.getItemName(),r.getSourceLocationName(),
            p==null?null:p.getId(),p==null?null:p.getLegalName(),s==null?null:s.getId(),s==null?null:s.getSiteName(),r.getMappedGodownCode(),
            r.getLocationType(),r.getQuantity(),r.getSnapshotDate(),r.getTargetStockBucket(),r.getOpeningTransactionType(),r.getValidationStatus(),
            r.getValidationMessage(),r.isDuplicateConfirmed(),r.isExcluded(),r.getExclusionReason(),
            r.getPostedStockTransaction()==null?null:r.getPostedStockTransaction().getId(),r.getVersion());}
    private String defaultCode(ItemMappingRequest r,StockImportRow row){return r.itemCode()==null||r.itemCode().isBlank()?
        "MAT-"+String.format("%03d",Integer.parseInt(row.getSourceSrNumber())):r.itemCode();}
    private String defaultName(ItemMappingRequest r,StockImportRow row){return r.itemName()==null||r.itemName().isBlank()?row.getNormalizedItemSuggestion():r.itemName();}
    private String sha256(byte[]bytes){try{byte[]hash=MessageDigest.getInstance("SHA-256").digest(bytes);return HexFormat.of().formatHex(hash);}
        catch(Exception e){throw new IllegalStateException(e);}}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}
    private void log(String action,StockImportBatch batch,String description,HttpServletRequest request){User user=users.findByUsernameIgnoreCase(actor()).orElse(null);
        audit.log(user==null?null:user.getId(),actor(),action,"StockImportBatch",String.valueOf(batch.getId()),safe(description),request);}
    private String safe(String value){return value==null?"Import operation failed":value.substring(0,Math.min(250,value.length()));}
    private String trim(String v){return v==null||v.isBlank()?null:v.trim();}private BusinessRuleException rule(String code,String message){return new BusinessRuleException(code,message);}
}
