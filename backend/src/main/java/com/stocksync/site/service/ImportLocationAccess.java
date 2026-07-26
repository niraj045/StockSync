package com.stocksync.site.service;
public interface ImportLocationAccess {
 LocationView resolve(Long partyId,Long siteId,boolean createParty,String partyName,boolean createSite,String siteName,String siteCode,String actor);
 record LocationView(Long partyId,String partyName,Long siteId,String siteName){}
}
