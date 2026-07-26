package com.stocksync.site.service;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.entity.*;
import com.stocksync.site.repository.SiteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ImportLocationService implements ImportLocationAccess {
 private final PartyRepository parties;private final SiteRepository sites;
 public ImportLocationService(PartyRepository parties,SiteRepository sites){this.parties=parties;this.sites=sites;}
 @Override @Transactional public LocationView resolve(Long partyId,Long siteId,boolean createParty,String partyName,
   boolean createSite,String siteName,String siteCode,String actor){
  Party party;
  if(createParty){if(partyName==null||partyName.isBlank())throw new BusinessRuleException("PARTY_NAME_REQUIRED","Party name is required");
   party=new Party();party.setLegalName(partyName.trim());party.setActive(true);party.setNotes("Created during legacy opening-stock mapping");
   party.setCreatedBy(actor);party.setUpdatedBy(actor);party=parties.save(party);}
  else party=parties.findById(partyId==null?-1:partyId).filter(Party::isActive)
   .orElseThrow(()->new BusinessRuleException("PARTY_NOT_FOUND_OR_INACTIVE","Select an active party"));
  Site site;
  if(createSite){if(siteName==null||siteName.isBlank()||siteCode==null||siteCode.isBlank())
    throw new BusinessRuleException("SITE_DETAILS_REQUIRED","Site name and site code are required");
   if(sites.existsBySiteCodeIgnoreCase(siteCode.trim()))throw new BusinessRuleException("SITE_CODE_ALREADY_EXISTS","Site code already exists");
   site=new Site();site.setParty(party);site.setSiteName(siteName.trim());site.setSiteCode(siteCode.trim().toUpperCase());
   site.setStatus(SiteStatus.ACTIVE);site.setNotes("Created during legacy opening-stock mapping");site.setCreatedBy(actor);site.setUpdatedBy(actor);site=sites.save(site);}
  else site=sites.findByIdAndPartyId(siteId==null?-1:siteId,party.getId())
   .filter(s->s.getStatus()!=SiteStatus.CLOSED).orElseThrow(()->new BusinessRuleException("SITE_NOT_FOUND","Select an open site under the mapped party"));
  return new LocationView(party.getId(),party.getLegalName(),site.getId(),site.getSiteName());
 }
}
