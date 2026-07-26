package com.stocksync.inventory.service;
import java.util.Optional;
public interface ImportItemAccess {
 Optional<ItemView> byId(Long id); Optional<ItemView> byCode(String code); Optional<ItemView> byExactName(String name);
 ItemView create(String itemCode,String itemName,String actor);
 record ItemView(Long id,String itemCode,String itemName,boolean active){}
}
