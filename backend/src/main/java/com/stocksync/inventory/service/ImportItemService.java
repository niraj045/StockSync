package com.stocksync.inventory.service;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
@Service
public class ImportItemService implements ImportItemAccess {
 private final ItemRepository items;private final ItemCategoryRepository categories;
 public ImportItemService(ItemRepository items,ItemCategoryRepository categories){this.items=items;this.categories=categories;}
 @Override @Transactional(readOnly=true)public Optional<ItemView>byId(Long id){return items.findById(id).map(this::view);}
 @Override @Transactional(readOnly=true)public Optional<ItemView>byCode(String code){return items.findByItemCodeIgnoreCase(code).map(this::view);}
 @Override @Transactional(readOnly=true)public Optional<ItemView>byExactName(String name){return items.findByItemNameIgnoreCase(name).map(this::view);}
 @Override @Transactional public ItemView create(String code,String name,String actor){
  if(code==null||code.isBlank()||name==null||name.isBlank())throw new BusinessRuleException("IMPORT_ITEM_DETAILS_REQUIRED","Item code and name are required");
  if(items.existsByItemCodeIgnoreCase(code.trim()))throw new BusinessRuleException("ITEM_CODE_ALREADY_EXISTS","Item code already exists");
  ItemCategory category=categories.findByNameIgnoreCase("SCAFFOLDING").orElseGet(()->{ItemCategory c=new ItemCategory();c.setName("SCAFFOLDING");
   c.setDescription("Scaffolding materials imported from legacy opening stock");c.setActive(true);c.setCreatedBy(actor);c.setUpdatedBy(actor);return categories.save(c);});
  Item item=new Item();item.setItemCode(code.trim().toUpperCase());item.setItemName(name.trim());item.setCategory(category);item.setUnit("PIECE");
  item.setMinimumStock(null);item.setActive(true);item.setCreatedBy(actor);item.setUpdatedBy(actor);return view(items.save(item));
 }
 private ItemView view(Item i){return new ItemView(i.getId(),i.getItemCode(),i.getItemName(),i.isActive());}
}
