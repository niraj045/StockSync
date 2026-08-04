package com.stocksync.workflow.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.workflow.dto.ClientWorkflowDtos.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientWorkflowService {
    private static final Set<String> INQUIRY_SOURCES=Set.of("EMAIL","PHONE","JUSTDIAL","REFERENCE","WALK_IN","OTHER");
    private static final Set<String> INQUIRY_STATUSES=Set.of("OPEN","FOLLOW_UP","QUOTED","WON","LOST","CANCELLED");
    private static final Set<String> OPERATION_TYPES=Set.of("TRANSPORT","LOADING_LABOUR","UNLOADING_LABOUR","MATHADI","TPI","CONTRACT_LABOUR","SITE_EXPENSE");
    private static final Set<String> DIRECTIONS=Set.of("DELIVERY","RETURN","GENERAL");
    private static final Set<String> OPERATION_STATUSES=Set.of("PLANNED","CONFIRMED","COMPLETED","CANCELLED");
    private final JdbcTemplate jdbc;
    public ClientWorkflowService(JdbcTemplate jdbc){this.jdbc=jdbc;}

    @Transactional(readOnly=true)
    public List<InquiryResponse> inquiries(String search,String status){
        List<Object> args=new ArrayList<>();StringBuilder where=new StringBuilder(" WHERE 1=1");
        if(search!=null&&!search.isBlank()){where.append(" AND (LOWER(i.inquiry_number) LIKE ? OR LOWER(i.contact_name) LIKE ? OR LOWER(i.requirement) LIKE ?)");String q="%"+search.trim().toLowerCase(Locale.ROOT)+"%";args.add(q);args.add(q);args.add(q);}
        if(status!=null&&!status.isBlank()){where.append(" AND i.status=?");args.add(upper(status));}
        return jdbc.query("""
            SELECT i.*,p.legal_name party_name,s.site_name,q.quotation_number
            FROM client_inquiries i LEFT JOIN parties p ON p.id=i.party_id LEFT JOIN sites s ON s.id=i.site_id
            LEFT JOIN quotations q ON q.id=i.quotation_id
            """+where+" ORDER BY i.inquiry_date DESC,i.id DESC LIMIT 500",(rs,rowNum)->inquiry(rs,rowNum),args.toArray());
    }

    @Transactional
    public InquiryResponse createInquiry(InquiryRequest r){validateInquiry(r);String actor=actor();
        KeyHolder key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("""
            INSERT INTO client_inquiries(inquiry_date,source,contact_name,phone,email,party_id,site_id,requirement,follow_up_date,status,quotation_id,notes,created_by,updated_by)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,Statement.RETURN_GENERATED_KEYS);bindInquiry(ps,r,actor);return ps;},key);
        long id=Objects.requireNonNull(key.getKey()).longValue();String number=String.format("INQ/%d/%05d",r.inquiryDate().getYear(),id);
        jdbc.update("UPDATE client_inquiries SET inquiry_number=? WHERE id=?",number,id);return inquiry(id);
    }

    @Transactional
    public InquiryResponse updateInquiry(long id,InquiryRequest r){validateInquiry(r);int count=jdbc.update("""
        UPDATE client_inquiries SET inquiry_date=?,source=?,contact_name=?,phone=?,email=?,party_id=?,site_id=?,requirement=?,follow_up_date=?,status=?,quotation_id=?,notes=?,updated_by=? WHERE id=?
        """,r.inquiryDate(),upper(r.source()),trim(r.contactName()),trim(r.phone()),trim(r.email()),r.partyId(),r.siteId(),trim(r.requirement()),r.followUpDate(),upper(r.status()),r.quotationId(),trim(r.notes()),actor(),id);
        if(count==0)throw error("INQUIRY_NOT_FOUND","Inquiry not found");return inquiry(id);
    }

    @Transactional(readOnly=true)
    public List<OperationResponse> operations(Long siteId,String operationType,String status){
        List<Object> args=new ArrayList<>();StringBuilder where=new StringBuilder(" WHERE 1=1");
        if(siteId!=null){where.append(" AND o.site_id=?");args.add(siteId);}if(operationType!=null&&!operationType.isBlank()){where.append(" AND o.operation_type=?");args.add(upper(operationType));}
        if(status!=null&&!status.isBlank()){where.append(" AND o.status=?");args.add(upper(status));}
        return jdbc.query(operationSql()+where+" ORDER BY o.operation_date DESC,o.id DESC LIMIT 500",(rs,rowNum)->operation(rs,rowNum),args.toArray());
    }

    @Transactional
    public OperationResponse createOperation(OperationRequest r){validateOperation(r);String actor=actor();KeyHolder key=new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(c->{PreparedStatement ps=c.prepareStatement("""
            INSERT INTO site_operations(operation_date,operation_type,direction,party_id,site_id,issued_challan_id,receiving_challan_id,provider_type,provider_name,transporter_name,vehicle_number,driver_name,worker_count,quantity,rate,amount,charge_to_client,status,reference_number,notes,created_by,updated_by)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,Statement.RETURN_GENERATED_KEYS);bindOperation(ps,r,actor);return ps;},key);
        long id=Objects.requireNonNull(key.getKey()).longValue();String number=String.format("OPS/%d/%05d",r.operationDate().getYear(),id);jdbc.update("UPDATE site_operations SET operation_number=? WHERE id=?",number,id);return operation(id);
    }

    @Transactional
    public OperationResponse updateOperation(long id,OperationRequest r){validateOperation(r);int count=jdbc.update("""
        UPDATE site_operations SET operation_date=?,operation_type=?,direction=?,party_id=?,site_id=?,issued_challan_id=?,receiving_challan_id=?,provider_type=?,provider_name=?,transporter_name=?,vehicle_number=?,driver_name=?,worker_count=?,quantity=?,rate=?,amount=?,charge_to_client=?,status=?,reference_number=?,notes=?,updated_by=? WHERE id=?
        """,r.operationDate(),upper(r.operationType()),upper(r.direction()),r.partyId(),r.siteId(),r.issuedChallanId(),r.receivingChallanId(),trim(r.providerType()),trim(r.providerName()),trim(r.transporterName()),trim(r.vehicleNumber()),trim(r.driverName()),r.workerCount(),r.quantity(),r.rate(),r.amount(),r.chargeToClient(),upper(r.status()),trim(r.referenceNumber()),trim(r.notes()),actor(),id);
        if(count==0)throw error("SITE_OPERATION_NOT_FOUND","Site operation not found");return operation(id);
    }

    private InquiryResponse inquiry(long id){return jdbc.query("SELECT i.*,p.legal_name party_name,s.site_name,q.quotation_number FROM client_inquiries i LEFT JOIN parties p ON p.id=i.party_id LEFT JOIN sites s ON s.id=i.site_id LEFT JOIN quotations q ON q.id=i.quotation_id WHERE i.id=?",rs->{if(!rs.next())throw error("INQUIRY_NOT_FOUND","Inquiry not found");return inquiry(rs,0);},id);}
    private OperationResponse operation(long id){return jdbc.query(operationSql()+" WHERE o.id=?",rs->{if(!rs.next())throw error("SITE_OPERATION_NOT_FOUND","Site operation not found");return operation(rs,0);},id);}
    private String operationSql(){return """
        SELECT o.*,p.legal_name party_name,s.site_name,ic.challan_number issued_challan_number,rc.receiving_challan_number
        FROM site_operations o LEFT JOIN parties p ON p.id=o.party_id JOIN sites s ON s.id=o.site_id
        LEFT JOIN issued_challans ic ON ic.id=o.issued_challan_id LEFT JOIN receiving_challans rc ON rc.id=o.receiving_challan_id
        """;}
    private InquiryResponse inquiry(ResultSet rs,int n)throws SQLException{return new InquiryResponse(rs.getLong("id"),rs.getString("inquiry_number"),rs.getObject("inquiry_date",LocalDate.class),rs.getString("source"),rs.getString("contact_name"),rs.getString("phone"),rs.getString("email"),nullableLong(rs,"party_id"),rs.getString("party_name"),nullableLong(rs,"site_id"),rs.getString("site_name"),rs.getString("requirement"),rs.getObject("follow_up_date",LocalDate.class),rs.getString("status"),nullableLong(rs,"quotation_id"),rs.getString("quotation_number"),rs.getString("notes"),rs.getString("created_by"),rs.getTimestamp("created_at").toInstant(),rs.getString("updated_by"),rs.getTimestamp("updated_at").toInstant());}
    private OperationResponse operation(ResultSet rs,int n)throws SQLException{return new OperationResponse(rs.getLong("id"),rs.getString("operation_number"),rs.getObject("operation_date",LocalDate.class),rs.getString("operation_type"),rs.getString("direction"),nullableLong(rs,"party_id"),rs.getString("party_name"),rs.getLong("site_id"),rs.getString("site_name"),nullableLong(rs,"issued_challan_id"),rs.getString("issued_challan_number"),nullableLong(rs,"receiving_challan_id"),rs.getString("receiving_challan_number"),rs.getString("provider_type"),rs.getString("provider_name"),rs.getString("transporter_name"),rs.getString("vehicle_number"),rs.getString("driver_name"),(Integer)rs.getObject("worker_count"),rs.getBigDecimal("quantity"),rs.getBigDecimal("rate"),rs.getBigDecimal("amount"),rs.getBoolean("charge_to_client"),rs.getString("status"),rs.getString("reference_number"),rs.getString("notes"),rs.getString("created_by"),rs.getTimestamp("created_at").toInstant(),rs.getString("updated_by"),rs.getTimestamp("updated_at").toInstant());}
    private void bindInquiry(PreparedStatement ps,InquiryRequest r,String actor)throws SQLException{Object[] v={r.inquiryDate(),upper(r.source()),trim(r.contactName()),trim(r.phone()),trim(r.email()),r.partyId(),r.siteId(),trim(r.requirement()),r.followUpDate(),upper(r.status()),r.quotationId(),trim(r.notes()),actor,actor};for(int i=0;i<v.length;i++)ps.setObject(i+1,v[i]);}
    private void bindOperation(PreparedStatement ps,OperationRequest r,String actor)throws SQLException{Object[] v={r.operationDate(),upper(r.operationType()),upper(r.direction()),r.partyId(),r.siteId(),r.issuedChallanId(),r.receivingChallanId(),trim(r.providerType()),trim(r.providerName()),trim(r.transporterName()),trim(r.vehicleNumber()),trim(r.driverName()),r.workerCount(),r.quantity(),r.rate(),r.amount(),r.chargeToClient(),upper(r.status()),trim(r.referenceNumber()),trim(r.notes()),actor,actor};for(int i=0;i<v.length;i++)ps.setObject(i+1,v[i]);}
    private void validateInquiry(InquiryRequest r){if(!INQUIRY_SOURCES.contains(upper(r.source())))throw error("INVALID_INQUIRY_SOURCE","Invalid inquiry source");if(!INQUIRY_STATUSES.contains(upper(r.status())))throw error("INVALID_INQUIRY_STATUS","Invalid inquiry status");if(r.followUpDate()!=null&&r.followUpDate().isBefore(r.inquiryDate()))throw error("INVALID_FOLLOW_UP_DATE","Follow-up cannot precede inquiry date");}
    private void validateOperation(OperationRequest r){if(!OPERATION_TYPES.contains(upper(r.operationType())))throw error("INVALID_OPERATION_TYPE","Invalid site operation type");if(!DIRECTIONS.contains(upper(r.direction())))throw error("INVALID_OPERATION_DIRECTION","Invalid operation direction");if(!OPERATION_STATUSES.contains(upper(r.status())))throw error("INVALID_OPERATION_STATUS","Invalid operation status");if(r.issuedChallanId()!=null&&r.receivingChallanId()!=null)throw error("MULTIPLE_CHALLAN_LINKS","Link either a delivery or return challan, not both");}
    private Long nullableLong(ResultSet rs,String c)throws SQLException{long v=rs.getLong(c);return rs.wasNull()?null:v;}private String upper(String v){return v==null?null:v.trim().toUpperCase(Locale.ROOT);}private String trim(String v){return v==null||v.isBlank()?null:v.trim();}
    private String actor(){var a=SecurityContextHolder.getContext().getAuthentication();return a==null?"system":a.getName();}private BusinessRuleException error(String c,String m){return new BusinessRuleException(c,m);}
}
